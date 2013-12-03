import models._
import org.joda.time.DateMidnight
import org.joda.time.DateTimeConstants._
import play.api.{GlobalSettings, Application}
import scala.{Some, None}

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    InitialData.insert()
  }
}

object InitialData {

  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

  def insert() = {

    val ARCHITECT: String = "architect"
    val POT: String = "pot"
    val QA: String = "qa"
    val POF: String = "pof"
    val USER_1: String = "user1"
    val USER_2: String = "user2"
    val USER_3: String = "user3"
    val USER_4: String = "user4"
    val USER_5: String = "user5"
    val USER_6: String = "user6"
    val USER_7: String = "user7"
    val USER_8: String = "user8"
    val USER_9: String = "user9"
    val USER_10: String = "user10"
    val USER_11: String = "user11"
    val USER_12: String = "user12"
    val USER_13: String = "user13"

    if (Iteration.list.isEmpty) {
      Seq(
        Iteration("IT 1", new DateMidnight(2013, SEPTEMBER, 2).toDate),
        Iteration("IT 2", new DateMidnight(2013, SEPTEMBER, 16).toDate),
        Iteration("IT 3", new DateMidnight(2013, OCTOBER, 7).toDate),
        Iteration("IT 4", new DateMidnight(2013, OCTOBER, 21).toDate),
        Iteration("IT 5", new DateMidnight(2013, NOVEMBER, 4).toDate),
        Iteration("IT 6", new DateMidnight(2013, NOVEMBER, 18).toDate),
        Iteration("IT 7", new DateMidnight(2013, DECEMBER, 2).toDate),
        Iteration("IT 8", new DateMidnight(2013, DECEMBER, 16).toDate),
        Iteration("IT 9", new DateMidnight(2013, JANUARY, 6).toDate),
        Iteration("IT 10", new DateMidnight(2013, JANUARY, 20).toDate),
        Iteration("IT 11", new DateMidnight(2013, FEBRUARY, 3).toDate),
        Iteration("IT 12", new DateMidnight(2013, FEBRUARY, 17).toDate),
        Iteration("IT 13", new DateMidnight(2013, MARCH, 3).toDate),
        Iteration("IT 14", new DateMidnight(2013, MARCH, 17).toDate),
        Iteration("IT 15", new DateMidnight(2013, MARCH, 31).toDate),
        Iteration("IT 16", new DateMidnight(2013, APRIL, 14).toDate),
        Iteration("IT 17", new DateMidnight(2013, APRIL, 28).toDate)
      ).foreach(Iteration.create)
    }


    if (User.findAll.isEmpty) {
      val DEFAULT_PASSWORD: String = "secret"
      Seq(
        User(POT, "pot@domain.dom", "Technical", "Product Owner", DEFAULT_PASSWORD, "pot", transverse = true, role = Some("Technical PO")),
        User(POF, "pof@domain.dom", "Functional", "Product Owner", DEFAULT_PASSWORD, "pof", transverse = true, role = Some("Functional PO")),
        User(QA, "qa@domain.dom", "Quality", "Assurance", DEFAULT_PASSWORD, "qa", transverse = true, role = Some("QA")),
        User(ARCHITECT, "architect@domain.dom", "Technical", "Architect", DEFAULT_PASSWORD, "architect", transverse = true, role = Some("Architect")),
        User(USER_1, "user1@domain.dom", "User", "1", DEFAULT_PASSWORD, "user1", transverse = false),
        User(USER_2, "user2@domain.dom", "User", "2", DEFAULT_PASSWORD, "user2", transverse = false),
        User(USER_3, "user3@domain.dom", "User", "3", DEFAULT_PASSWORD, "user3", transverse = false),
        User(USER_4, "user4@domain.dom", "User", "4", DEFAULT_PASSWORD, "user4", transverse = false),
        User(USER_5, "user5@domain.dom", "User", "5", DEFAULT_PASSWORD, "user5", transverse = false),
        User(USER_6, "user6@domain.dom", "User", "6", DEFAULT_PASSWORD, "user6", transverse = false),
        User(USER_7, "user7@domain.dom", "User", "7", DEFAULT_PASSWORD, "user7", transverse = false),
        User(USER_8, "user8@domain.dom", "User", "8", DEFAULT_PASSWORD, "user8", transverse = false),
        User(USER_9, "user9@domain.dom", "User", "9", DEFAULT_PASSWORD, "user9", transverse = false),
        User(USER_10, "user10@domain.dom", "User", "10", DEFAULT_PASSWORD, "user10", transverse = false),
        User(USER_11, "user11@domain.dom", "User", "11", DEFAULT_PASSWORD, "user11", transverse = false),
        User(USER_12, "user12@domain.dom", "User", "12", DEFAULT_PASSWORD, "user12", transverse = false),
        User(USER_13, "user13@domain.dom", "User", "13", DEFAULT_PASSWORD, "user13", transverse = false)
      ).foreach(User.create)
    }

    val TEAM_1: String = "Team1"
    val TEAM_2: String = "Team2"
    val TEAM_3: String = "Team3"
    val STEERING: String = "Steering"
    if (Team.findAll.isEmpty) {
      Seq(
        Team(TEAM_1, Some(USER_1)) -> Seq(USER_1, USER_2, USER_3, USER_4),
        Team(TEAM_2, Some(USER_5)) -> Seq(USER_5, USER_6, USER_7, USER_8),
        Team(TEAM_3, Some(USER_9)) -> Seq(USER_9, USER_10, USER_11, USER_12, USER_13),
        Team(STEERING, Option.empty, steering = true) -> Seq(POT, QA, POT, ARCHITECT)
      ).foreach {
        case (team, members) => Team.create(team, members)
      }
    }

    if (Guest.list.isEmpty) {
      Seq(
        Guest(TEAM_1, None, 1),
        Guest(TEAM_1, Some(POT), 2),
        Guest(TEAM_1, Some(ARCHITECT), 3),
        Guest(TEAM_1, Some(POT), 4),
        Guest(TEAM_1, Some(POT), 5),
        Guest(TEAM_1, Some(QA), 6),
        Guest(TEAM_1, Some(POT), 7),
        Guest(TEAM_1, Some(ARCHITECT), 8),
        Guest(TEAM_1, Some(QA), 9),
        Guest(TEAM_1, None, 10),
        Guest(TEAM_2, None, 1),
        Guest(TEAM_2, Some(QA), 2),
        Guest(TEAM_2, Some(POT), 3),
        Guest(TEAM_2, Some(QA), 4),
        Guest(TEAM_2, Some(ARCHITECT), 5),
        Guest(TEAM_2, Some(POT), 6),
        Guest(TEAM_2, Some(QA), 7),
        Guest(TEAM_2, Some(POT), 8),
        Guest(TEAM_2, Some(ARCHITECT), 9),
        Guest(TEAM_2, None, 10),
        Guest(TEAM_3, None, 1),
        Guest(TEAM_3, Some(ARCHITECT), 2),
        Guest(TEAM_3, Some(QA), 3),
        Guest(TEAM_3, Some(POT), 4),
        Guest(TEAM_3, Some(QA), 5),
        Guest(TEAM_3, Some(ARCHITECT), 6),
        Guest(TEAM_3, Some(POT), 7),
        Guest(TEAM_3, Some(QA), 8),
        Guest(TEAM_3, Some(POT), 9),
        Guest(TEAM_3, None, 10)
      ).foreach(Guest.create)
    }
  }
}


