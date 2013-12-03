package models

import anorm.SqlParser._
import play.api.db.DB
import anorm._
import play.api.Play.current
import commons.DateHelper
import java.util.Date

case class Team(name: String, scrumMaster: Option[String], steering: Boolean = false) {
  lazy val members: Seq[User] = Team.membersOf(name)

  def lastMeetings(nbMeetings: Int = 5): Seq[Meeting] = Meeting.lastMeetingsForTeam(name, nbMeetings)

  def todayMeeting: Option[Meeting] = meeting(DateHelper.today)

  def tomorrowMeeting: Option[Meeting] = meeting(DateHelper.nextDay)

  def meeting(date: Date): Option[Meeting] = Meeting.getMeeting(date, name)

  def groups: Seq[String] = if (steering) Team.groupsForSteering else List(name)

  lazy val scrumMasterUser: Option[User] = if (scrumMaster.isDefined) User.findByLogin(scrumMaster.get) else None
}

object Team {

  // -- Parsers
  val simple = {
    get[String]("team.name") ~
      get[Option[String]]("team.scrum_master") ~
      get[Boolean]("team.steering") map {
      case name ~ scrumMaster ~ steering => Team(name, scrumMaster, steering)
    }
  }


  // -- Queries
  def findAll: Seq[Team] = DB.withConnection {
    implicit connection =>
      SQL("select * from team order by steering, name").as(Team.simple *)
  }

  def findAllExceptSteering: Seq[Team] = DB.withConnection {
    implicit connection =>
      SQL("select * from team where steering = false").as(Team.simple *)
  }

  def groupsForSteering: Seq[String] = DB.withConnection {
    implicit connection =>
      SQL("select team.name from team where steering = false").as(str("name") *)
  }

  /**
   * Retrieve a Team from name.
   */
  def findByName(name: String): Option[Team] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from team where name = {name}").on(
          'name -> name
        ).as(Team.simple.singleOpt)
    }
  }

  /**
   * Retrieve teams for user
   */
  def findInvolving(user: User): Seq[Team] = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
            select * from team
            join team_member on team.name = team_member.team
            where team_member.user_login = {login}
          """
        ).on(
          'login -> user.login
        ).as(Team.simple *)
    }
  }

  /**
   * Change a team scrum master.
   */
  def changeScrumMaster(name: String, newScrumMaster: String) {
    DB.withConnection {
      implicit connection =>
        SQL("update team set scrum_master = {scrumMaster} where name = {name}").on(
          'name -> name, 'scrumMaster -> newScrumMaster
        ).executeUpdate()
    }
  }

  /**
   * Delete a team.
   */
  def delete(name: String) {
    DB.withConnection {
      implicit connection =>
        SQL("delete from team where name = {name}").on(
          'name -> name
        ).executeUpdate()
    }
  }

  /**
   * Retrieve team member
   */
  def membersOf(team: String): Seq[User] = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          select users.* from users
          join team_member on team_member.user_login = users.login
          where team_member.team = {team}
          """
        ).on(
          'team -> team
        ).as(User.simple *)
    }
  }

  /**
   * Add a member to the team.
   */
  def addMember(team: String, user: String) {
    DB.withConnection {
      implicit connection =>
        SQL("insert into team_member values({team}, {user})").on(
          'team -> team,
          'user -> user
        ).executeUpdate()
    }
  }

  /**
   * Remove a member from the team.
   */
  def removeMember(team: String, user: String) {
    DB.withConnection {
      implicit connection =>
        SQL("delete from team_member where team_id = {team} and user_login = {user}").on(
          'team -> team,
          'user -> user
        ).executeUpdate()
    }
  }

  /**
   * Check if a user is a member of this team
   */
  def isMember(team: String, user: String): Boolean = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          select count(users.login) = 1 from users
          join team_member on team_member.user_login = users.login
          where team_member.team = {team} and users.login = {login}
          """
        ).on(
          'team -> team,
          'login -> user
        ).as(scalar[Boolean].single)
    }
  }

  /**
   * Create a Team.
   */
  def create(team: Team, members: Seq[String]): Team = {
    DB.withTransaction {
      implicit connection =>

      // Insert the team
        SQL(
          """
           insert into team values (
             {name}, {scrumMaster}, {steering}
           )
          """
        ).on(
          'name -> team.name,
          'scrumMaster -> team.scrumMaster,
          'steering -> team.steering
        ).executeUpdate()

        // Add members
        members.foreach {
          login =>
            SQL("insert into team_member (team, user_login) values ({team}, {login})").on('team -> team.name, 'login -> login).executeUpdate()
        }

        team

    }
  }

}
