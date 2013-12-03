package controllers

import play.api.mvc.{WebSocket, Action, Controller}
import models._
import play.api.data._
import play.api.data.Forms._
import views.{txt, html}
import commons.DateHelper
import java.util.Date
import play.api.libs.json.JsValue
import scala.Some

object Meetings extends Controller with Secured {

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

  def details(team: String, dateStr: String) = IsAuthenticated {
    username => implicit request =>
      User.findByLogin(username).map {
        user =>
            Ok(
              html.meetings.details(
                Team.findInvolving(user),
                user,
                Meeting.getOrCreateMeeting(DateHelper.fromUrl(dateStr), team)
              )
            )

      }.getOrElse(Forbidden)
  }

  def detailsTxt(team: String, dateStr: String) = IsAuthenticated {
    username => implicit request =>
      User.findByLogin(username).map {
        user =>
            Ok(
              txt.meetings.detailsTxt(
                Team.findInvolving(user),
                user,
                Meeting.getOrCreateMeeting(DateHelper.fromUrl(dateStr), team)
              )
            )

      }.getOrElse(Forbidden)
  }

  def detailsConfluence(team: String, dateStr: String) = IsAuthenticated {
    username => implicit request =>
      User.findByLogin(username).map {
        user =>
            Ok(
              txt.meetings.detailsConfluence(
                Team.findInvolving(user),
                user,
                Meeting.getOrCreateMeeting(DateHelper.fromUrl(dateStr), team)
              )
            )

      }.getOrElse(Forbidden)
  }

  def steering(team: String, dateStr: String) = IsAuthenticated {
    username => implicit request =>
      User.findByLogin(username).map {
        user =>
            Ok(
              html.meetings.steering(
                Team.findInvolving(user),
                user,
                Meeting.getOrCreateMeeting(DateHelper.fromUrl(dateStr), team)
              )
            )

      }.getOrElse(Forbidden)
  }

  def steeringTxt(team: String, dateStr: String) = IsAuthenticated {
    username => implicit request =>
      User.findByLogin(username).map {
        user =>
            Ok(
              txt.meetings.steeringTxt(
                Team.findInvolving(user),
                user,
                Meeting.getOrCreateMeeting(DateHelper.fromUrl(dateStr), team)
              )
            )

      }.getOrElse(Forbidden)
  }

  def meetingRoom(username:String, team: String, dateStr: String) = WebSocket.async[JsValue] { request  =>

    MeetingRoom.join(username, team, DateHelper.fromUrl(dateStr))

  }
}
