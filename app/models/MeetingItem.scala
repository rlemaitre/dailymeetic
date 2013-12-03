package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import anorm.~
import play.api.Play.current


case class MeetingItem(id: Option[Long], meetingId: Long, jiraIssue: Option[String], content: Option[String], impediment: Boolean, bug: Boolean, grouping:String) {
  lazy val meeting = Meeting.getMeeting(meetingId)

}

object MeetingItem {
  val meetingItem = {
    get[Option[Long]]("id") ~
      get[Long]("meeting") ~
      get[Option[String]]("jiraIssue") ~
      get[Option[String]]("content") ~
      get[Boolean]("impediment") ~
      get[Boolean]("bug") ~
      get[String]("grouping") map {
      case id ~ meeting ~ jiraIssue ~ content ~ impediment ~ bug ~ grouping => MeetingItem(id, meeting, jiraIssue, content, impediment, bug, grouping)
    }
  }

  def create(item: MeetingItem): MeetingItem =
    DB.withConnection {
      implicit connection =>

      // Get the team id
        val id: Long = item.id.getOrElse {
          SQL("select nextval('meeting_item_seq')").as(scalar[Long].single)
        }

        // Insert the team
        SQL(
          """
           insert into meeting_item (id, meeting, jiraIssue, content, impediment, bug, grouping) values (
             {id}, {meeting}, {jiraIssue}, {content}, {impediment}, {bug}, {grouping}
           )
          """
        ).on(
          'id -> id,
          'meeting -> item.meetingId,
          'jiraIssue -> item.jiraIssue,
          'content -> item.content,
          'impediment -> item.impediment,
          'bug -> item.bug,
          'grouping -> item.grouping
        ).executeUpdate()

        item.copy(id = Some(id))
    }

  def delete(id: Long) = DB.withConnection {
    implicit c =>
      SQL("delete from meeting_item where id = {id}").on('id -> id).executeUpdate()
  }

  def editContent(id: Long, content: Option[String]) = DB.withConnection {
    implicit c =>
      SQL("update meeting_item set content = {content} where id = {id}").on('id -> id, 'content -> content).executeUpdate()
  }

  def editJiraIssue(id: Long, jiraIssue: Option[String]) = DB.withConnection {
    implicit c =>
      SQL("update meeting_item set jiraIssue = {jiraIssue} where id = {id}").on('id -> id, 'jiraIssue -> jiraIssue).executeUpdate()
  }

  def editBug(id: Long, bug: Boolean) = DB.withConnection {
    implicit c =>
      SQL("update meeting_item set bug = {bug} where id = {id}").on('id -> id, 'bug -> bug).executeUpdate()
  }

  def editImpediment(id: Long, impediment: Boolean) = DB.withConnection {
    implicit c =>
      SQL("update meeting_item set impediment = {impediment} where id = {id}").on('id -> id, 'impediment -> impediment).executeUpdate()
  }

  def list(meetingId:Option[Long]): Seq[MeetingItem] = DB.withConnection {
    implicit c =>
      SQL("select * from meeting_item where meeting = {meetingId} order by id").on('meetingId -> meetingId).as(meetingItem *)
  }

  def addParticipant(meetingId: Long, userLogin: String) = DB.withConnection {
    implicit connection => SQL("insert into meeting_member(meeting_id, user_login) values ({meetingId}, {userLogin})")
      .on('meetingId -> meetingId, 'userLogin -> userLogin).executeUpdate()
  }

  def removeParticipant(meetingId: Long, userLogin: String) = DB.withConnection {
    implicit connection => SQL("delete from meeting_member where meeting_id = {meetingId} and user_login= {userLogin}")
      .on('meetingId -> meetingId, 'userLogin -> userLogin).executeUpdate()
  }


}
