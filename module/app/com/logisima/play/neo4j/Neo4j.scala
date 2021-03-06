package com.logisima.play.neo4j

import play.api.Play
import play.Logger
import play.api.Play.current

import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.server.configuration.{Configurator, ServerConfigurator}
import org.neo4j.server.WrappingNeoServerBootstrapper

import org.neo4j.shell.ShellSettings
import org.neo4j.helpers.Settings

/**
 * Neo4j database object.
 *
 * @author : bsimard
 */
object Neo4j {

  /**
   * Graph database
   */
  var graphdb :Option[GraphDatabaseAPI] = None

  /**
   * Server for webadmin
   */
  var webadmin :Option[WrappingNeoServerBootstrapper] = None

  /**
   * Starting neo4j database.
   *
   * @return
   */
  private def startDb() = {
    val DBPath :String = Play.configuration.getString("neo4j.path").getOrElse("neo4j")
    Logger.debug("Neo4j database path is :" + DBPath)

    val graphdb = (new GraphDatabaseFactory())
                .newEmbeddedDatabaseBuilder( DBPath )
                .setConfig( ShellSettings.remote_shell_enabled, Settings.TRUE )
                .newGraphDatabase()
                .asInstanceOf[GraphDatabaseAPI];

    val config = new ServerConfigurator( graphdb );
    // let the server endpoint be on a custom port
    config.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, 7575 );

    val srv = new WrappingNeoServerBootstrapper( graphdb, config );
    
    (graphdb, srv)
  }

  /**
   * Starting webadmin server.
   *
   * @return
   */
  private def startAdmin() = {
    Logger.debug("Starting webadmin")
    webadmin.map { server =>
      server.start()
    }
  }

  /**
   * Start the database with primary nodes.
   */
  def start() {
    Logger.debug("init database")
    
    Some(startDb()).map{ case (x,y) =>
      graphdb = Some(x)
      webadmin = Some(y)
    }
    startAdmin()
  }

  /**
   * Stopping server & admin server
   */
  def stop() {
    Logger.debug("Shutting down webadmin")
    webadmin.foreach(_.stop())
    Logger.debug("Shutting down neo4j")
    graphdb.foreach(_.shutdown())
  }

}
