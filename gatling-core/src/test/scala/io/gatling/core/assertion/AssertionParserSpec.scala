/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.assertion

import org.scalacheck.{ Arbitrary, Gen }

import io.gatling.BaseSpec

trait AssertionGenerator {
  this: AssertionParserSpec =>

  private val intGen = Arbitrary.arbitrary[Int]

  private val pathGen = {

    val detailsGen = for (parts <- Gen.nonEmptyListOf(Gen.alphaStr.suchThat(_.length > 0))) yield Details(parts)
    Gen.frequency(33 -> Gen.const(Global), 33 -> Gen.const(ForAll), 33 -> detailsGen)
  }

  private val targetGen = {
    val countTargetGen = {
      val countMetricGen = Gen.oneOf(AllRequests, FailedRequests, SuccessfulRequests)
      val countSelectionGen = Gen.oneOf(Count, Percent, PerMillion)

      for (metric <- countMetricGen; selection <- countSelectionGen) yield CountTarget(metric, selection)
    }

    val timeTargetGen = {
      val timeMetricGen = Gen.const(ResponseTime)
      val timeSelectionGen = Gen.oneOf(Min, Max, Mean, StandardDeviation, Percentiles1, Percentiles2, Percentiles3, Percentiles4)

      for (metric <- timeMetricGen; selection <- timeSelectionGen) yield TimeTarget(metric, selection)
    }

    Gen.oneOf(countTargetGen, timeTargetGen, Gen.const(MeanRequestsPerSecondTarget))
  }

  private val conditionGen = {
    val lessThan = for (d <- intGen) yield LessThan(d)
    val greaterThan = for (d <- intGen) yield GreaterThan(d)
    val is = for (d <- intGen) yield Is(d)
    val between = for (d1 <- intGen; d2 <- intGen) yield Between(d1, d2)
    val in = for (doubleList <- Gen.nonEmptyListOf(intGen)) yield In(doubleList)

    Gen.oneOf(lessThan, greaterThan, is, between, in)
  }

  val assertionGen: Gen[Assertion] = for {
    path <- pathGen
    target <- targetGen
    condition <- conditionGen
  } yield Assertion(path, target, condition)
}
class AssertionParserSpec extends BaseSpec with AssertionGenerator {

  override implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 300)

  "The assertion parser" should "be able to parse correctly arbitrary assertions" in {
    forAll(assertionGen) { assertion =>
      val parser = new AssertionParser
      parser.parseAssertion(assertion.serialized.toString) shouldBe assertion
    }
  }
}
