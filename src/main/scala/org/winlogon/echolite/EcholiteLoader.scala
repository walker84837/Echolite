// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import io.papermc.paper.plugin.loader.{PluginClasspathBuilder, PluginLoader}
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class EcholiteLoader extends PluginLoader {
    override def classloader(classpathBuilder: PluginClasspathBuilder): Unit = {
        val scalaVersion = "3.3.5"
        val jdaVersion = "5.3.2"

        val resolver = MavenLibraryResolver()

        resolver.addRepository(
            RemoteRepository.Builder(
                "central", 
                "default", 
                "https://repo.maven.apache.org/maven2/"
            ).build()
        )

        resolver.addDependency(
            Dependency(
                DefaultArtifact(s"org.scala-lang:scala3-library_3:$scalaVersion"),
                null
            )
        )

        resolver.addDependency(
            Dependency(
                DefaultArtifact(s"net.dv8tion:JDA:$jdaVersion"),
                null
            )
        )

        resolver.addDependency(
            Dependency(
                DefaultArtifact("dev.vankka:mcdiscordreserializer:4.3.0"),
                null
            )
        )

        classpathBuilder.addLibrary(resolver)
    }
}

