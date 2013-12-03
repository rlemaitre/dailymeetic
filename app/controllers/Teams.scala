package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._


import models._
import views._
import play.api.Logger

object Teams extends Controller with Secured {

  /**
   * Display the dashboard.
   */
  def index = IsAuthenticated {
    username => _ =>
      User.findByLogin(username).map {
        user =>
          Ok(
            html.dashboard(
              Team.findInvolving(user),
              user
            )
          )
      }.getOrElse(Forbidden)
  }

  /**
   * Display the team details.
   */
  def details(teamName: String) = IsAuthenticated {
    username => _ =>
      User.findByLogin(username).map {
        user => {
          Team.findByName(teamName) match {
            case Some(team) => Ok(
              html.teams.details(
                Team.findInvolving(user),
                user,
                team
              )
            )
            case None => Forbidden
          }
        }
      }.getOrElse(Forbidden)
  }

  /**
   * Display the team settings.
   */
  def settings(teamName: String) = IsAuthenticated {
    username => _ =>
      User.findByLogin(username).map {
        user => {
          Team.findByName(teamName) match {
            case Some(team) => Ok(
              html.teams.settings(
                Team.findInvolving(user),
                user,
                team,
                settingsForm.fill(team.scrumMaster.getOrElse(""))
              )
            )
            case None => Forbidden
          }
        }
      }.getOrElse(Forbidden)
  }

  // -- Teams

  /**
   * Add a team.
   */
  def add = IsAuthenticated {
    username => implicit request =>
      Form("group" -> nonEmptyText).bindFromRequest.fold(
        errors => BadRequest,
        folder => Ok
      )
  }

  /**
   * Delete a team.
   */
  def delete(team: String) = IsMemberOf(team) {
    username => _ =>
      Team.delete(team)
      Ok
  }


  // -- Members

  /**
   * Add a team member.
   */
  def addUser(team: String) = IsMemberOf(team) {
    _ => implicit request =>
      Form("user" -> nonEmptyText).bindFromRequest.fold(
        errors => BadRequest,
        user => {
          Team.addMember(team, user);
          Ok
        }
      )
  }

  /**
   * Remove a team member.
   */
  def removeUser(team: String) = IsMemberOf(team) {
    _ => implicit request =>
      Form("user" -> nonEmptyText).bindFromRequest.fold(
        errors => BadRequest,
        user => {
          Team.removeMember(team, user);
          Ok
        }
      )
  }

  /**
   * Handle form submission.
   */
  def changeSettings(teamName: String) = {
    IsAuthenticated {
      username => implicit request => {
        User.findByLogin(username).map {
          user => {
            val teamOption: Option[Team] = Team.findByName(teamName)
            if (teamOption.isDefined)
              settingsForm.bindFromRequest.fold(
                errors => {
                  errors.errors.map(e => Logger.error(e.key + " : " + e.message))
                  BadRequest(html.teams.settings(Team.findInvolving(user), user, teamOption.get, errors))
                },
                edited => {
                  edited match {
                    case scrumMaster => Team.changeScrumMaster(teamName, scrumMaster)
                  }
                  val editedTeam: Team = Team.findByName(teamName).get
                  Ok(html.teams.settings(
                    Team.findInvolving(user),
                    user,
                    editedTeam,
                    settingsForm.fill((editedTeam.scrumMaster.getOrElse("")))
                  ))
                }
              )
            else Forbidden
          }

        }.getOrElse(Forbidden)
      }
    }
  }

  val settingsForm = Form(
    "scrumMaster" -> nonEmptyText
  )

}
