/*
 *    Copyright 2017 Ilya Epifanov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "kafka-tool"

val circeVersion = "0.8.0"
val log4jVersion = "2.10.0"

libraryDependencies ++= Seq(
  "org.apache.kafka" %% "kafka" % "1.0.0",
  "com.typesafe" % "config" % "1.3.2",
  "com.iheart" %% "ficus" % "1.4.3",
  "org.rogach" %% "scallop" % "3.1.1",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion
)

excludeDependencies ++= Seq(
  "log4j" % "log4j"
)
