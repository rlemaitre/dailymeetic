package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import java.util.Date
import anorm.~
import commons.DateHelper
import play.api.Logger
import java.sql.Connection

case class Meeting(id: Option[Long], date: Date, teamName: String, topic: Option[String], problem: Option[Boolean]) {
  lazy val team: Team = Team.findByName(teamName).get

  def participants: Seq[User] = Meeting.getMeetingParticipants(id)

  def items: Seq[MeetingItem] = MeetingItem.list(id)

  def itemsByGroup: Map[String, Seq[MeetingItem]] = items.groupBy(i => i.grouping)

  def previousMeeting: Option[Meeting] = Meeting.getMeeting(DateHelper.previousDay(date), teamName)

  def nextMeeting: Option[Meeting] = Meeting.getMeeting(DateHelper.nextDay(date), teamName)
}

object Meeting {

  val empty: Meeting = Meeting(None, DateHelper.today, "", None, None)

  val meeting = {
    get[Option[Long]]("id") ~
      get[Date]("meeting_date") ~
      get[String]("team") ~
      get[Option[String]]("topic") ~
      get[Option[Boolean]]("problem") map {
      case id ~ date ~ team ~ topic ~ problem => Meeting(id, date, team, topic, problem)
    }
  }

  def meetingsForTeam(team: String): List[Meeting] = DB.withConnection {
    implicit c =>
      SQL("select * from meeting where team = {team} order by meeting_date desc").on('team -> team).as(meeting *)
  }

  def lastMeetingsForTeam(team: String, nb: Int): List[Meeting] = DB.withConnection {
    implicit c =>
      SQL("select * from meeting where team = {team} order by meeting_date desc limit {nbMeeting}").on('team -> team, 'nbMeeting -> nb).as(meeting *)
  }

  def getMeetingParticipants(meetingId: Option[Long]): Seq[User] = DB.withConnection {
    implicit c =>
      SQL("select users.* from users join meeting_member on users.login = meeting_member.user_login where meeting_member.meeting_id = {meetingId}").on('meetingId -> meetingId).as(User.simple *)
  }

  def getMeeting(date: Date, team: String): Option[Meeting] = DB.withConnection {
    implicit c =>
      SQL("select * from meeting where team = {team} and meeting_date = {date}").on('team -> team, 'date -> date).as(meeting.singleOpt)
  }

  def getMeeting(id: Long): Option[Meeting] = DB.withConnection {
    implicit c =>
      SQL("select * from meeting where id = {id}").on('id -> id).as(meeting.singleOpt)
  }

  def getOrCreateMeeting(date: Date, team: String): Meeting =
    Meeting.getMeeting(date, team).getOrElse(Meeting.create(date, team))

  def initMeetingItemWithPreviousItems(meeting: Meeting) = {
    def items: Seq[MeetingItem] =
      if (meeting.team.steering)
        (for (
          team <- Team.findAllExceptSteering;
          m <- Meeting.getMeeting(meeting.date, team.name)
        ) yield m.items).flatten
      else
        meeting.previousMeeting.map(m => m.items).getOrElse(Seq())
    Logger("Meeting").error(items.toString())
    items.foreach {
      item => MeetingItem.create(MeetingItem(None, meeting.id.get, item.jiraIssue, item.content, item.impediment, item.bug, item.grouping))
    }

  }

  def editProblem(id: Long, problem: Boolean) = DB.withConnection {
    implicit c =>
      SQL("update meeting set problem = {problem} where id = {id}").on('id -> id, 'problem -> problem).executeUpdate()
  }

  def create(date: Date, team: String): Meeting = {
    DB.withConnection {
      implicit connection =>

      // Get the meeting id
        val id: Long = SQL("select nextval('meeting_seq')").as(scalar[Long].single)

        // Insert the meeting
        SQL(
          """
           insert into meeting (id, meeting_date, team) values (
             {id}, {date}, {team}
           )
          """
        ).on(
          'id -> id,
          'date -> date,
          'team -> team
        ).executeUpdate()

        val newMeeting: Meeting = Meeting(Some(id), date, team, Option.empty, Option.empty)
        initMeetingMembers(newMeeting)
        initMeetingItemWithPreviousItems(newMeeting)
        newMeeting
    }
  }

  def create(date: Date, team: String, problem: Boolean): Meeting = {
    DB.withConnection {
      implicit connection =>

      // Get the meeting id
        val id: Long = SQL("select nextval('meeting_seq')").as(scalar[Long].single)

        // Insert the meeting
        SQL(
          """
           insert into meeting (id, meeting_date, team, problem) values (
             {id}, {date}, {team}, {problem}
           )
          """
        ).on(
          'id -> id,
          'date -> date,
          'team -> team,
          'problem -> problem
        ).executeUpdate()

        val newMeeting: Meeting = Meeting(Some(id), date, team, Option.empty, Option(problem))
        initMeetingMembers(newMeeting)
        newMeeting
    }
  }

  def initMeetingMembers(meeting: Meeting) = DB.withConnection {
    implicit connection =>

    // Add team members
      meeting.team.members.foreach {
        user => insertParticipant(meeting, user)
      }
      if (meeting.team.steering) {
        Team.findAllExceptSteering.foreach {
          team => if (team.scrumMasterUser.isDefined) {
            insertParticipant(meeting, team.scrumMasterUser.get)
          }
        }
      } else if (configuration.includeTransverseInTeamMeeting) {
        val guest: Option[User] = Guest.forMeeting(meeting)
        if (guest.isDefined)
          insertParticipant(meeting, guest.get)
      }
  }

  private def insertParticipant(meeting: Meeting, user: User)(implicit connection: Connection): Int = {
    SQL("insert into meeting_member (meeting_id, user_login) values ({meetingId}, {login})").on('meetingId -> meeting.id, 'login -> user.login).executeUpdate()
  }

  def create(meeting: Meeting) {
    DB.withConnection {
      implicit connection =>

      // Get the team id
        val id: Long = meeting.id.getOrElse {
          SQL("select nextval('meeting_seq')").as(scalar[Long].single)
        }

        // Insert the team
        SQL(
          """
           insert into meeting (id, meeting_date, team, topic, problem) values (
             {id}, {date}, {team}, {topic}, {problem}
           )
          """
        ).on(
          'id -> id,
          'date -> meeting.date,
          'team -> meeting.teamName,
          'topic -> meeting.topic,
          'problem -> meeting.problem
        ).executeUpdate()

        meeting.copy(id = Some(id))
    }
  }

  def delete(id: Long) {
    DB.withConnection {
      implicit c =>
        SQL("delete from meeting where id = {id}").on(
          'id -> id
        ).executeUpdate()
    }
  }
}
