package org.ublog.persistence

import io.github.gaelrenoux.tranzactio.DbException

object models {
  case class DbPost(id: String, title: String, content: String, author: String)
  sealed trait PersistenceException extends Throwable

  object PersistenceException {
    def apply(dbException: DbException): PersistenceException = new PersistenceException {
      override def getMessage: String  = dbException.getMessage
      override def getCause: Throwable = dbException.getCause
    }

    def apply(throwable: Throwable): PersistenceException = new PersistenceException {
      override def getMessage: String  = throwable.getMessage
      override def getCause: Throwable = throwable.getCause
    }
  }
}
