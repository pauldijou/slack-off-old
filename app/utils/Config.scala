package utils

import play.api.Play

trait Config {
  def config = Config.config
  def getString(key: String) = Config.getString(key)

  def jiraUrl = Config.jiraUrl
  def jiraAuthBasic = Config.jiraAuthBasic

  def slackTeam = Config.slackTeam
  def slackTokenIncoming = Config.slackTokenIncoming
}

object Config {
  lazy val config = Play.current.configuration
  def getString(key: String): String = config.getString(key) getOrElse ""

  lazy val jiraUrl = getString("jira.url")
  lazy val jiraAuthBasic = getString("jira.auth.basic")

  lazy val slackTeam = getString("slack.team.name")
  lazy val slackTokenIncoming = getString("slack.hooks.incoming.token")
}
