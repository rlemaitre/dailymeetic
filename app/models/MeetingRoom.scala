package models

import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Date
import commons.DateHelper
import com.typesafe.plugin._

object MeetingRoom {
  implicit val timeout = Timeout(1 second)

  lazy val default = {
    val roomActor = Akka.system.actorOf(Props[MeetingRoom])

    roomActor
  }

  def join(username: String, team: String, date: Date): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {

    (default ? Join(username)).map {

      case Connected(enumerator) =>

        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] {
          event =>
            default ! Talk(username, event)
        }.map {
          _ =>
            default ! Quit(username)
        }

        (iteratee, enumerator)

      case CannotConnect(error) =>

        // Connection error

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)

        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)

    }

  }
}

class MeetingRoom extends Actor {
  var members = Set.empty[String]
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  implicit val creatureWrites = new Writes[MeetingItem] {
    def writes(c: MeetingItem): JsValue = {
      Json.obj(
        "bug" -> c.bug,
        "content" -> c.content,
        "id" -> c.id,
        "impediment" -> c.impediment,
        "jiraIssue" -> c.jiraIssue,
        "meetingId" -> c.meetingId,
        "grouping" -> c.grouping
      )
    }
  }

  def receive = {

    case Join(username) => {
      if (members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        members = members + username
        sender ! Connected(chatEnumerator)
        self ! NotifyJoin(username)
      }
    }

    case NotifyJoin(username) => {
      notifyAll("join", username, JsString("has entered the room"))
    }

    case Talk(username, jsonItem) => {
      Logger("meeting room").error("received " + jsonItem)


      val operation: String = (jsonItem \ "operation").as[String]
      val dateStr: String = (jsonItem \ "date").as[String]
      val team: String = (jsonItem \ "team").as[String]
      val grouping: String = (jsonItem \ "grouping").as[String]

      operation match {
        case "addItem" =>
          processItemCreation(dateStr, team, jsonItem, username, grouping)
        case "deleteItem" =>
          processItemDeletion(dateStr, team, jsonItem, username, grouping)
        case "editItemContent" =>
          processEditContent(dateStr, team, jsonItem, username, grouping)
        case "editItemJira" =>
          processEditJira(dateStr, team, jsonItem, username, grouping)
        case "editItemBug" =>
          processEditBug(dateStr, team, jsonItem, username, grouping)
        case "editItemImpediment" =>
          processEditImpediment(dateStr, team, jsonItem, username, grouping)
        case "editProblem" =>
          processProblem(dateStr, team, jsonItem, username, grouping)
        case "switchParticipant" =>
          processSwitchParticipant(dateStr, team, jsonItem, username, grouping)
        case "sendMinutes" =>
          sendMinutes(dateStr, team, jsonItem, username, grouping)
      }
    }

    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, JsString("has left the room"))
    }
  }

  private def processItemCreation(dateStr: String, team: String, jsonItem: JsValue, username: String, grouping: String) {
    val meeting: Option[Meeting] = Meeting.getMeeting(DateHelper.fromUrl(dateStr), team)
    val meetingId: Long = meeting.get.id.get
    val meetingItem = readMeetingItem(jsonItem, meetingId)
    val item: MeetingItem = MeetingItem.create(meetingItem)
    val sentJsonItem: JsObject = Json.obj("team" -> team, "date" -> dateStr, "grouping" -> grouping, "operation" -> "addItem", "item" -> Json.toJson(item))
    notifyAll("item", username, sentJsonItem)
  }

  private def processItemDeletion(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val id: Long = (jsonItem \ "id").as[Long]
    MeetingItem.delete(id)
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def sendMinutes(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val meeting: Meeting = Meeting.getMeeting(DateHelper.fromUrl(dateStr), teamName).get
    val mail = use[MailerPlugin].email
    mail.setSubject("[Steering] " + DateHelper.readableDate(meeting.date))
    User.findAll.map(u => u.email).foreach(address => mail.addRecipient(address))

    mail.addFrom("Daily Meeting <raphael.lemaitre@systar.com>")

    val buf = new StringBuilder
    buf ++= "<html><body>"
    meeting.items.groupBy(_.grouping).foreach(pair => {
      buf ++= "<html><body><h1>" + pair._1 + "</h1>\n"
      buf ++= "<ul>"
      pair._2.foreach(item => {
        buf ++= "<li " + (if (item.bug) {
          "style=\"color: red\""
        }) + ">" + (if (item.bug) {
          "BUG "
        }) + item.jiraIssue.getOrElse("") + " : " + item.content.getOrElse("") + "<li>"
      }
      )
      buf ++= "</ul>"
    }
    )
    buf ++= "</body></html>"
    //sends text/text
    mail.sendHtml(buf.toString)
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def processEditContent(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val id: Long = (jsonItem \ "id").as[Long]
    val content: Option[String] = (jsonItem \ "content").asOpt[String]
    MeetingItem.editContent(id, content)
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def processEditBug(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val id: Long = (jsonItem \ "id").as[Long]
    val content: Boolean = (jsonItem \ "bug").as[Boolean]
    MeetingItem.editBug(id, content)
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def processEditImpediment(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val id: Long = (jsonItem \ "id").as[Long]
    val content: Boolean = (jsonItem \ "impediment").as[Boolean]
    MeetingItem.editImpediment(id, content)
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def processSwitchParticipant(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val add: Boolean = (jsonItem \ "add").as[Boolean]
    val userToAdd: String = (jsonItem \ "user").as[String]
    val id = Meeting.getMeeting(DateHelper.fromUrl(dateStr), grouping).get.id.get
    if (add)
      MeetingItem.addParticipant(id, userToAdd)
    else
      MeetingItem.removeParticipant(id, userToAdd)
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def processProblem(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val id: Long = (jsonItem \ "id").as[Long]
    val problem: Boolean = (jsonItem \ "problem").as[Boolean]
    val meetingId: Long =
      if (teamName == grouping)
        id
      else
        Meeting.getMeeting(DateHelper.fromUrl(dateStr), grouping).getOrElse(Meeting.empty).id.getOrElse(0)
    Meeting.editProblem(meetingId, problem)
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def processEditJira(dateStr: String, teamName: String, jsonItem: JsValue, username: String, grouping: String) = {
    val id: Long = (jsonItem \ "id").as[Long]
    val jiraIssue: Option[String] = (jsonItem \ "jiraIssue").asOpt[String]
    MeetingItem.editJiraIssue(id, jiraIssue match {
      case Some("") => None
      case _ => jiraIssue
    })
    Logger("meeting room").error("sending " + jsonItem)
    notifyAll("item", username, jsonItem)
  }

  private def readMeetingItem(jsonItem: JsValue, meetingId: Long): MeetingItem = {
    val itemId: Option[Long] = (jsonItem \ "item" \ "id").asOpt[Long]
    val jiraIssue: Option[String] = (jsonItem \ "item" \ "jiraIssue").asOpt[String]
    val content: Option[String] = (jsonItem \ "item" \ "content").asOpt[String]
    val impediment: Boolean = (jsonItem \ "item" \ "impediment").asOpt[Boolean].fold(false)(b => b)
    val bug: Boolean = (jsonItem \ "item" \ "bug").asOpt[Boolean].fold(false)(b => b)
    val grouping: String = (jsonItem \ "item" \ "grouping").as[String]
    MeetingItem(itemId, meetingId, jiraIssue, content, impediment, bug, grouping)
  }

  def notifyAll(kind: String, user: String, message: JsValue) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> message,
        "members" -> JsArray(
          members.toList.map(JsString)
        )
      )
    )
    Logger("meeting room").error("sending " + msg)
    chatChannel.push(msg)
  }

}

case class Join(username: String)

case class Quit(username: String)

case class Talk(username: String, message: JsValue)

case class NotifyJoin(username: String)

case class Connected(enumerator: Enumerator[JsValue])

case class CannotConnect(msg: String)

