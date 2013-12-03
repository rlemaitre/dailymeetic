package controllers

import models._
import views._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Logger

object Users extends Controller with Secured {

  def profile(login: String) = IsAuthenticated {
    username => _ => {
      val displayedUser: Option[User] = User.findByLogin(login)
      val loggedUser: Option[User] = User.findByLogin(username)
      (displayedUser, loggedUser) match {
        case (Some(user), Some(logged)) =>
          Ok(
            html.profile(
              Team.findInvolving(logged),
              logged,
              userProfileForm.fill((user.login, user.email, user.firstName, user.lastName, user.skype, user.transverse)),
              userPasswordForm.fill("", "", "")
            )
          )
        case (_, _) => Forbidden
      }
    }
  }

  /**
   * Handle form submission.
   */
  def submit(login: String) = {
    IsAuthenticated {
      username => implicit request => {
        User.findByLogin(username).map {
          user =>
            userProfileForm.bindFromRequest.fold(
              errors => {
                errors.errors.map(e => Logger.error(e.key + " : " + e.message))
                BadRequest(html.profile(Team.findInvolving(user), user, errors, userPasswordForm.fill("", "", "")))
              },
              edited => {
                edited match {
                  case (editedLogin, editedEmail, firstName, lastName, skype, transverse) => User.updateUser(editedLogin, editedEmail, firstName, lastName, transverse, skype)
                }
                val update: User = User.findByLogin(login).get
                val user2: User = User.findByLogin(username).get
                Ok(html.profile(
                  Team.findInvolving(user2),
                  user2,
                  userProfileForm.fill((update.login, update.email, update.firstName, update.lastName, update.skype, update.transverse)),
                  userPasswordForm.fill("", "", "")
                ))
              }
            )

        }.getOrElse(Forbidden)
      }
    }
  }


  val userProfileForm = Form(
    tuple(
      "login" -> nonEmptyText,
      "email" -> email,
      "firstName" -> nonEmptyText(maxLength = 255),
      "lastName" -> nonEmptyText(maxLength = 255),
      "skype" -> nonEmptyText,
      "transverse" -> boolean
    )
  )

  val userPasswordForm = Form(
    tuple(
      "old_password" -> nonEmptyText(maxLength = 255),
      "password" -> nonEmptyText(maxLength = 255),
      "password_conf" -> nonEmptyText(maxLength = 255)
    )
  )

  def changePassword(login: String) = IsAuthenticated {
    username => implicit request => {
      User.findByLogin(username).map {
        user =>
          userPasswordForm.bindFromRequest.fold(
            errors => {
              errors.errors.map(e => Logger.error(e.key + " : " + e.message))
              val update: User = User.findByLogin(login).get
              BadRequest(
                html.profile(
                  Team.findInvolving(user),
                  user,
                  userProfileForm.fill((update.login, update.email, update.firstName, update.lastName, update.skype, update.transverse)),
                  errors))
            },
            edited => {
              edited match {
                case (oldPassword, password, passwordConfirmation) => {
                  val userToUpdate: User = User.findByLogin(login).get
                  if (userToUpdate.password == oldPassword)
                    if (password == passwordConfirmation) {
                      User.changePassword(login, password)
                      html.profile(
                        Team.findInvolving(user),
                        user,
                        userProfileForm.fill((userToUpdate.login, userToUpdate.email, userToUpdate.firstName, userToUpdate.lastName, userToUpdate.skype, userToUpdate.transverse)),
                        userPasswordForm.fill("", "", ""))
                    } else {
                      BadRequest(
                        html.profile(
                          Team.findInvolving(user),
                          user,
                          userProfileForm.fill((userToUpdate.login, userToUpdate.email, userToUpdate.firstName, userToUpdate.lastName, userToUpdate.skype, userToUpdate.transverse)),
                          userPasswordForm.fill("", "", "").withGlobalError("password", "New Password does not match confirmation")))
                    }
                  else {
                    BadRequest(
                      html.profile(
                        Team.findInvolving(user),
                        user,
                        userProfileForm.fill((userToUpdate.login, userToUpdate.email, userToUpdate.firstName, userToUpdate.lastName, userToUpdate.skype, userToUpdate.transverse)),
                        userPasswordForm.fill("", "", "").withGlobalError("password", "Please enter correct old password")))
                  }
                }
              }
              val update: User = User.findByLogin(login).get
              val user2: User = User.findByLogin(username).get
              Ok(html.profile(
                Team.findInvolving(user2),
                user2,
                userProfileForm.fill((update.login, update.email, update.firstName, update.lastName, update.skype, update.transverse)),
                userPasswordForm.fill("", "", "")
              ))
            }
          )

      }.getOrElse(Forbidden)
    }
  }

}
