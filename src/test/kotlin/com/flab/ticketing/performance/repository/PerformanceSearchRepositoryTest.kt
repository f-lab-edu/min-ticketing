package com.flab.ticketing.performance.repository

import com.flab.ticketing.testutils.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.conditions.NonCiEnvironment
import com.flab.ticketing.testutils.config.ElasticSearchConfiguration
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime


/**
 * CI/CD 환경에서는 Docker를 활용한 TestContainer 사용이 불가능 하기 때문에 로컬에서만 테스트하도록 설정하였습니다.
 * */
@SpringBootTest
@Import(ElasticSearchConfiguration::class)
@EnabledIf(NonCiEnvironment::class)
@ActiveProfiles("test")
class PerformanceSearchRepositoryTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)


    @Autowired
    private lateinit var performanceSearchRepository: PerformanceSearchRepository


    init {
        "공연 정보를 조건 없이 조회할 수 있다." {
            // given
            val performanceDocuments =
                PerformanceTestDataGenerator.createPerformanceGroupbyRegion(performanceCount = 5)
                    .map { PerformanceSearchSchema.of(it) }

            performanceSearchRepository.saveAll(performanceDocuments)

            //when
            val foundDocuments = performanceSearchRepository.findAll()

            //then
            foundDocuments shouldContainAll performanceDocuments
        }

        "DB에 Performance List가 limit 이상의 갯수를 저장하고 있다면, 올바르게 limit개 만큼의 데이터를 갖고 올 수 있다." {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 10,
                numShowtimes = 1,
                seatPerPlace = 1
            ).map { PerformanceSearchSchema.of(it) }


            performanceSearchRepository.saveAll(performances)

            val limit = 5
            val actual = performanceSearchRepository.search(PerformanceSearchConditions(), null, limit)

            actual.data.size shouldBe limit
        }

        "다음 커서 정보를 받아 다음 데이터들을 가져올 수 있다." {
            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 10,
                numShowtimes = 1,
                seatPerPlace = 1
            ).map { PerformanceSearchSchema.of(it) }


            performanceSearchRepository.saveAll(performances)


            val limit = 5
            val searchResult1 =
                performanceSearchRepository.search(PerformanceSearchConditions(), null, limit)

            val searchResult2 =
                performanceSearchRepository.search(PerformanceSearchConditions(), searchResult1.cursor, limit)

            searchResult1.data + searchResult2.data shouldContainAll performances
        }

        "Performance를 Region 이름으로 필터링하여 조회할 수 있다." {

            //given
            val seoulRegionPerformances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "서울",
                performanceCount = 3
            ).map { PerformanceSearchSchema.of(it) }

            val gumiPerformanceCount = 3

            val gumiRegionPerformances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "구미",
                performanceCount = gumiPerformanceCount
            ).map { PerformanceSearchSchema.of(it) }

            performanceSearchRepository.saveAll(seoulRegionPerformances)
            performanceSearchRepository.saveAll(gumiRegionPerformances)

            // when
            val regionName = gumiRegionPerformances[0].region
            val actual = performanceSearchRepository.search(
                PerformanceSearchConditions(region = regionName),
                null,
                10
            ).data


            actual.size shouldBe gumiPerformanceCount
            actual.map { it.id } shouldContainAll gumiRegionPerformances.map { it.id }
        }

        "Performance를 최소 금액으로 필터링하여 조회할 수 있다." {

            // given
            val givenPriceRange = listOf(2000, 3000, 4000)
            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                priceIn = givenPriceRange
            ).map { PerformanceSearchSchema.of(it) }

            performanceSearchRepository.saveAll(performances)


            // when
            val minPrice = 3000
            val actual =
                performanceSearchRepository.search(PerformanceSearchConditions(minPrice = minPrice), null, 10).data

            actual.size shouldBe 2
            actual.filterNotNull().map { it.id } shouldContainAll listOf(
                performances[1].id,
                performances[2].id
            )
        }

        "Performance를 최대 금액으로 필터링하여 조회할 수 있다." {

            val givenPriceRange = listOf(2000, 3000, 4000)
            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                priceIn = givenPriceRange
            ).map { PerformanceSearchSchema.of(it) }


            performanceSearchRepository.saveAll(performances)

            val maxPrice = 3000
            val actual =
                performanceSearchRepository.search(PerformanceSearchConditions(maxPrice = maxPrice), null, 10).data

            actual.size shouldBe 2

            actual.filterNotNull().map { it.id } shouldContainAll listOf(
                performances[0].id,
                performances[1].id
            )
        }

        "Performance를 공연 날짜로 필터링하여 조회할 수 있다." {

            // 2024-1-1 10:00(Asia/Seoul) 공연 정보
            val performance1 =
                PerformanceSearchSchema.of(
                    PerformanceTestDataGenerator.createPerformance(
                        showTimeStartDateTime = ZonedDateTime.of(
                            LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                            ZoneId.of("Asia/Seoul")
                        ),
                        numShowtimes = 2
                    )
                )

            // 2023-1-1 10:00(Asia/Seoul) 공연 정보
            val performance2 =
                PerformanceSearchSchema.of(
                    PerformanceTestDataGenerator.createPerformance(
                        showTimeStartDateTime = ZonedDateTime.of(
                            LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                            ZoneId.of("Asia/Seoul")
                        ),
                        numShowtimes = 2
                    )
                )

            performanceSearchRepository.saveAll(listOf(performance1, performance2))

            // 2024-1-1 00:00시(Asia/Seoul)로 검색
            val searchShowTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZoneId.of("Asia/Seoul")
            )

            val actual = performanceSearchRepository.search(
                PerformanceSearchConditions(showTime = searchShowTime),
                null,
                10
            ).data

            actual.size shouldBe 1
            actual[0].id shouldBe performance1.id

        }

        "Performance를 공연 이름으로 필터링하여 조회할 수 있다." {
            val givenNames = listOf("예쁜 공연", "멋진 공연", "아주 멋진 공연", "공연 멋진", "공연")


            val performances = PerformanceTestDataGenerator.createPerformancesInNames(
                nameIn = givenNames
            ).map { PerformanceSearchSchema.of(it) }


            performanceSearchRepository.saveAll(performances)

            val actual = performanceSearchRepository.search(PerformanceSearchConditions(q = "멋진"), null, 10).data

            actual.size shouldBe 3
            actual.map { it.title } shouldContainAll listOf("멋진 공연", "아주 멋진 공연", "공연 멋진")

        }

        "Performance를 모든 조건을 넣어 검색할 수 있다." {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance1Price = 50000
            val performance1Name = "공공 공연"

            val performance1 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = performance1Price,
                name = performance1Name
            )

            val performance2 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = 10000,
                name = performance1Name
            )

            val performance3 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                price = performance1Price,
                name = performance1Name
            )

            val performance4 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                price = 15000
            )

            performanceSearchRepository.saveAll(
                listOf(
                    performance1,
                    performance2,
                    performance3,
                    performance4
                ).map { PerformanceSearchSchema.of(it) })

            val actual = performanceSearchRepository.search(
                PerformanceSearchConditions(
                    performance1DateTime,
                    performance1Price - 1000,
                    performance1Price + 1000,
                    region.name,
                    q = "공공"
                ),
                null,
                10
            ).data

            actual.size shouldBe 1
            actual[0].id shouldBe performance1.uid

        }

        "Performance 검색 결과가 존재하지 않을 시, 빈 data 배열과 null cursor를 반환한다." {
            // given

            // when
            val (cursor, data) = performanceSearchRepository.search(PerformanceSearchConditions(), null, 10)


            // then
            cursor shouldBe null
            data.size shouldBe 0

        }

    }

    override suspend fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)
        withContext(Dispatchers.IO) {
            performanceSearchRepository.deleteAll()
        }
    }

}