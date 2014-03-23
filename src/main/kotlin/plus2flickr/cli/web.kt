package plus2flickr.cli.web

import org.kohsuke.args4j.Option
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import com.google.inject.servlet.GuiceFilter
import javax.servlet.DispatcherType
import java.util.EnumSet
import plus2flickr.guice.ServicesModule
import plus2flickr.guice.DbModule
import plus2flickr.thirdparty.flickr.FlickrAppSettings
import java.io.IOException
import java.util.Properties
import java.io.StringReader
import plus2flickr.thirdparty.google.GoogleAppSettings
import com.google.inject.servlet.GuiceServletContextListener
import com.google.inject.Injector
import com.google.inject.Guice
import plus2flickr.couchdb.CouchDbManager
import org.codehaus.jackson.map.ObjectMapper
import plus2flickr.guice.WebServicesModule
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.ContextHandler

data class StartWebOptions(
    Option("-p") var port: Int = 8080,
    Option("-couchDb") var couchDb: String = "http://localhost:5984",
    Option("-flickrAppSettings") var flickrAppSettings: String? = null,
    Option("-googleAppSettings") var googleAppSettings: String? = null,
    Option("-staticContentPath") var staticContentPath: String = "src/main/webapp/app") {

  private fun readResource(path: String): String {
    val resource = javaClass<StartWebOptions>().getResource(path)
    if (resource == null) {
      throw IOException("Resource $path not found")
    }
    return resource.readText()
  }

  fun getFlickrAppSettings(): FlickrAppSettings {
    val settings = flickrAppSettings ?: readResource("/flickr_app.json")
    return ObjectMapper().readValue(settings.getBytes(), javaClass<FlickrAppSettings>())!!
  }

  fun getGoogleAppSettings(): GoogleAppSettings {
    val web = ObjectMapper().readTree(googleAppSettings ?: readResource("/client_secret.json"))!!.get("web")!!
    return GoogleAppSettings(
        clientId =  web.get("client_id")!!.asText()!!,
        clientSecret = web.get("client_secret")!!.asText()!!,
        applicationName = "Plus2Flickr",
        scopes = listOf(
            "https://www.googleapis.com/auth/plus.login",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://picasaweb.google.com/data/")
    )
  }
}

fun start(options: StartWebOptions) {
  val servicesContext = ServletContextHandler()
  servicesContext.setContextPath("/services")
  servicesContext.addEventListener(object : GuiceServletContextListener() {
    override fun getInjector(): Injector? {
      val injector = Guice.createInjector(
          WebServicesModule(),
          DbModule(url = options.couchDb),
          ServicesModule(options.getGoogleAppSettings(), options.getFlickrAppSettings()))
      injector!!.getInstance(javaClass<CouchDbManager>())!!.ensureDbExists()
      return injector
    }
  })
  servicesContext.addFilter(javaClass<GuiceFilter>(), "/*", EnumSet.of(DispatcherType.REQUEST))

  // TODO(amatyushentsev): stop serving static content in app server
  val resourcesHandler = ResourceHandler()
  resourcesHandler.setResourceBase(options.staticContentPath)
  val staticContext = ContextHandler("/")
  staticContext.setHandler(resourcesHandler)

  var handlers = HandlerList()
  handlers.setHandlers(array(servicesContext, staticContext))

  val server = Server(options.port)
  server.setHandler(handlers)
  server.start()
  server.join()
}