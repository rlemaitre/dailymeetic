package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.{Team, User}
import views.html

object Application extends Controller {

  // -- Authentication

  val loginForm = Form(
    tuple(
      "login" -> text,
      "password" -> text
    ) verifying ("Invalid login or password", result => result match {
      case (login, password) => User.authenticate(login, password).isDefined
    })
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Teams.index).withSession("login" -> user._1)
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

}

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("login")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)

  // --

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

  /**
   * Check if the connected user is a member of this project.
   */
  def IsMemberOf(team: String)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { username => request =>
    if(Team.isMember(team, username)) {
      f(username)(request)
    } else {
      Results.Forbidden
    }
  }


}
