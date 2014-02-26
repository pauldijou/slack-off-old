package utils

import play.api.Play

trait Config {
  lazy val config = Play.current.configuration

  def getString(key: String): String = config.getString(key) getOrElse ""

  def jiraUrl = getString("jira.url")
  def jiraAuthBasic = getString("jira.auth.basic")

  def slackTeam = getString("slack.team.name")
  def slackTokenIncoming = getString("slack.hooks.incoming.token")
}
