package photomover.web

import javax.servlet.http.HttpServletResponse
import photomover.domain.User
import photomover.services.UserService
import javax.servlet.http.HttpServletRequest
import com.google.inject.servlet.RequestScoped
import com.google.common.base.Suppliers
import com.google.inject.Inject

RequestScoped class RequestState[Inject] (
    val request: HttpServletRequest,
    val response: HttpServletResponse,
    val userService: UserService) {

  var user: User? = null

  val currentUser : User
      get() = user!!
}