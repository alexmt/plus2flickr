package photomover.web.resources

import photomover.web.models.OperationResponse
import photomover.thirdparty.AuthorizationException
import photomover.web.models.UserInfoViewModel
import javax.ws.rs.Path
import javax.ws.rs.POST
import javax.ws.rs.GET
import photomover.domain.User
import photomover.web.RequestState
import photomover.services.UserService
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.google.common.base.CharMatcher
import org.scribe.model.OAuthConstants
import javax.ws.rs.PathParam
import photomover.web.models.DetailedUserInfoViewModel
import photomover.domain.UserInfo
import photomover.web.models.ErrorInfo
import photomover.CloudServiceContainer
import photomover.web.models.OAuth2VerifyData
import com.google.inject.Inject

Path("/user") Produces("application/json")
class UserResource [Inject] (
    val userService: UserService, val state: RequestState, val services: CloudServiceContainer) {

  private fun User.getUserInfo(): UserInfoViewModel {
    val name = if (info.firstName != null && info.lastName != null) {
      "${info.firstName} ${info.lastName}"
    } else {
      info.firstName ?: info.lastName ?: "User"
    }
    val accountsState = hashMapOf<String, Boolean>()
    for (serviceCode in services.serviceCodes.sortBy { it }) {
      val oAuthData = accounts.get(serviceCode)
      accountsState.put(serviceCode, oAuthData != null && oAuthData.isTokenNeedRefresh == false)
    }

    return UserInfoViewModel(name, accountsState)
  }

  GET Path("/info") fun info(): UserInfoViewModel {
    return state.currentUser.getUserInfo()
  }

  GET Path("/detailedInfo") fun detailedInfo(): DetailedUserInfoViewModel {
    val info = state.currentUser.info
    return DetailedUserInfoViewModel(
        firstName = info.firstName ?: "",
        lastName = info.lastName ?: "",
        email = info.email ?: "",
        linkedServices = state.currentUser.accounts.entrySet()
            .filter { !it.value.isTokenNeedRefresh }
            .map { it.key })
  }

  POST Path("/updateInfo")
  fun updateInfo(info: DetailedUserInfoViewModel) {
    userService.updateUserInfo(state.currentUser, UserInfo(
        firstName = info.firstName,
        lastName = info.lastName,
        email = info.email
    ))
  }

  POST Path("removeService") fun removeService(service: String) =
      userService.removeService(state.currentUser, service)

  POST Path("deleteAccount") fun deleteAccount(Context response: HttpServletResponse) {
    userService.deleteUser(state.currentUser)
    logout(response)
  }

  POST Path("/verifyOAuth2") fun verifyOAuth2(data: OAuth2VerifyData)
      : OperationResponse<UserInfoViewModel> {
    try {
      userService.authorizeOAuth2Service(state.currentUser, data.code, data.service)
      return OperationResponse(data = state.currentUser.getUserInfo(), success = true)
    } catch (e: AuthorizationException){
      return OperationResponse(success = false, errors = listOf(ErrorInfo(message = e.errorMessage)))
    }
  }

  GET Path("/{service}/authorizeOAuth") fun authorizeOAuth(
      PathParam("service") service: String,
      Context request: HttpServletRequest,
      Context response: HttpServletResponse) {
    val path = CharMatcher.anyOf("/").trimFrom(request.getRequestURI().replace("/authorize", "/verify"))
    val url ="${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}/$path"
    response.sendRedirect(userService.getOAuthAuthorizationUrl(state.currentUser, url, service))
  }

  GET Path("/{service}/verifyOAuth") fun verifyOAuth(
      PathParam("service") service: String,
      Context request: HttpServletRequest,
      Context response: HttpServletResponse) {
    val authCode = request.getParameter(OAuthConstants.TOKEN).toString()
    val verifier = request.getParameter(OAuthConstants.VERIFIER).toString()
    userService.authorizeOAuthService(state.currentUser, authCode, verifier, service)
    response.sendRedirect("/")
  }

  GET Path("/logout") fun logout(Context response: HttpServletResponse) {
    state.user = null
    response.sendRedirect("/")
  }
}
