package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import scala.language.postfixOps
import play.api.Logger

case class User(login: String, email: String, firstName: String, lastName: String, password: String, skype: String, transverse: Boolean, role: Option[String] = None) {
  def completeName: String = firstName + " " + lastName
}

object User {
  // -- Parsers

  /**
   * Parse a User from a ResultSet
   */
  val simple = {
    get[String]("users.login") ~
      get[String]("users.email") ~
      get[String]("users.firstName") ~
      get[String]("users.lastName") ~
      get[String]("users.password") ~
      get[String]("users.skype") ~
      get[Boolean]("users.transverse") ~
      get[Option[String]]("users.role") map {
      case login ~ email ~ firstName ~ lastName ~ password ~ skype ~ transverse ~ role =>
        User(login, email, firstName, lastName, password, skype, transverse, role)
    }
  }

  // -- Queries

  /**
   * Retrieve a User from email.
   */
  def findByLogin(login: String): Option[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from users where login = {login}").on(
          'login -> login
        ).as(User.simple.singleOpt)
    }
  }

  /**
   * Retrieve a User from email.
   */
  def findByEmail(email: String): Option[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from users where email = {email}").on(
          'email -> email
        ).as(User.simple.singleOpt)
    }
  }

  /**
   * Retrieve all users.
   */
  def findAll: Seq[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from users order by lastname, firstname").as(User.simple *)
    }
  }

  /**
   * Authenticate a User.
   */
  def authenticate(login: String, password: String): Option[User] = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
         select * from users where
         login = {login} and password = {password}
          """
        ).on(
          'login -> login,
          'password -> password
        ).as(User.simple.singleOpt)
    }
  }

  /**
   * Create a User.
   */
  def create(user: User): User = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          insert into users(login, email, firstName, lastName, password, skype, transverse, role) values (
            {login}, {email}, {firstName}, {lastName}, {password}, {skype}, {transverse}, {role}
          )
          """
        ).on(
          'login -> user.login,
          'email -> user.email,
          'firstName -> user.firstName,
          'lastName -> user.lastName,
          'password -> user.password,
          'skype -> user.skype,
          'transverse -> user.transverse,
          'role -> user.role
        ).executeUpdate()

        user

    }
  }

  def updateUser(login: String, email: String, firstName: String, lastName: String, transverse: Boolean, skype: String) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update users set
            email = {email},
            firstname = {firstName},
            lastname = {lastName},
            skype = {skype},
            transverse = {transverse}
          where
            login = {login}
          """
        ).on(
          'email -> email,
          'firstName -> firstName,
          'lastName -> lastName,
          'skype -> skype,
          'transverse -> transverse,
          'login -> login
        ).executeUpdate()
    }
  }

  def changePassword(login: String, password: String) = DB.withConnection {
    implicit connection =>
      SQL(
        """
          update users set
            password = {password}
          where
            login = {login}
        """
      ).on(
        'password -> password,
        'login -> login
      ).executeUpdate()
  }


  /**
   * Updates a User.
   */
  def update(user: User): User = {
    Logger.error("updating user " + user)
    val login: String = user.login
    val email: String = user.email
    val firstName: String = user.firstName
    val lastName: String = user.lastName
    val transverse: Boolean = user.transverse
    val skype: String = user.skype

    updateUser(login, email, firstName, lastName, transverse, skype)
    user
  }


}
