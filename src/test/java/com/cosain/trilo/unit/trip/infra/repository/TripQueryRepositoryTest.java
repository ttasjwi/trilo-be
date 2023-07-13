
package com.cosain.trilo.unit.trip.infra.repository;

import com.cosain.trilo.fixture.TripFixture;
import com.cosain.trilo.fixture.UserFixture;
import com.cosain.trilo.support.RepositoryTest;
import com.cosain.trilo.trip.domain.entity.Trip;
import com.cosain.trilo.trip.infra.dto.TripDetail;
import com.cosain.trilo.trip.infra.dto.TripStatistics;
import com.cosain.trilo.trip.infra.dto.TripSummary;
import com.cosain.trilo.trip.infra.repository.trip.TripQueryRepository;
import com.cosain.trilo.trip.presentation.trip.dto.request.TripPageCondition;
import com.cosain.trilo.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@DisplayName("TripQueryRepository 테스트")
public class TripQueryRepositoryTest {

    @Autowired
    private TripQueryRepository tripQueryRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DirtiesContext
    void findTripDetailTest() {
        // given
        Long tripperId = setupTripperId();
        LocalDate startDate = LocalDate.of(2023, 3, 1);
        LocalDate endDate = LocalDate.of(2023, 3, 2);

        Trip trip = TripFixture.decided_nullId(tripperId, startDate, endDate);
        em.persist(trip);
        trip.getDays().forEach(em::persist);

        // when
        TripDetail tripDetail = tripQueryRepository.findTripDetailById(1L).get();

        // then
        assertThat(tripDetail.getTitle()).isEqualTo(trip.getTripTitle().getValue());
        assertThat(tripDetail.getTripperId()).isEqualTo(trip.getTripperId());
        assertThat(tripDetail.getTripId()).isEqualTo(trip.getId());
        assertThat(tripDetail.getStartDate()).isEqualTo(trip.getTripPeriod().getStartDate());
        assertThat(tripDetail.getEndDate()).isEqualTo(trip.getTripPeriod().getEndDate());
        assertThat(tripDetail.getStatus()).isEqualTo(trip.getStatus().name());
    }

    @Nested
    @DisplayName("사용자의 여행 목록을 조회 하면")
    class findTripDetailListByTripperIdTest {

        @Test
        @DirtiesContext
        @DisplayName("여행의 TipperId가 일치하는 여행들이 커서에 해당하는 tripId 미만 row가 size 만큼 조회된다")
        void findTest() {
            // given
            Long tripperId = setupTripperId();

            Trip trip1 = TripFixture.undecided_nullId(tripperId);
            Trip trip2 = TripFixture.undecided_nullId(tripperId);
            em.persist(trip1);
            em.persist(trip2);

            System.out.printf("trip ids = [%d, %d]%n", trip1.getId(), trip2.getId());
            em.flush();
            em.clear();

            int size = 2;
            Long tripId = trip2.getId() + 1L;

            TripPageCondition tripPageCondition = new TripPageCondition(tripperId, tripId);
            Pageable pageable = PageRequest.ofSize(size);

            // when
            Slice<TripSummary> tripSummariesByTripperId = tripQueryRepository.findTripSummariesByTripperId(tripPageCondition, pageable);

            // then
            assertThat(tripSummariesByTripperId.getContent().size()).isEqualTo(2);
        }

        @Test
        @DirtiesContext
        @DisplayName("가장 최근에 생성된 여행 순으로 조회된다")
        void sortTest() {
            // given
            Long tripperId = setupTripperId();
            Long tripId = 3L;
            TripPageCondition tripPageCondition = new TripPageCondition(tripperId, tripId);
            Pageable pageable = PageRequest.ofSize(3);

            Trip trip1 = TripFixture.undecided_nullId(tripperId);
            Trip trip2 = TripFixture.undecided_nullId(tripperId);
            em.persist(trip1);
            em.persist(trip2);
            em.flush();
            em.clear();

            // when
            Slice<TripSummary> tripSummariesByTripperId = tripQueryRepository.findTripSummariesByTripperId(tripPageCondition, pageable);


            // then
            assertThat(tripSummariesByTripperId.getContent().get(0).getTitle()).isEqualTo(trip2.getTripTitle().getValue());
            assertThat(tripSummariesByTripperId.getContent().get(1).getTitle()).isEqualTo(trip1.getTripTitle().getValue());
        }

        @Test
        @DirtiesContext
        void existByIdTest() {
            // given
            Long tripperId = setupTripperId();

            Trip trip = TripFixture.undecided_nullId(tripperId);
            em.persist(trip);
            em.flush();
            em.clear();

            // when & then
            long notExistTripId = 2L;
            assertThat(tripQueryRepository.existById(trip.getId())).isTrue();
            assertThat(tripQueryRepository.existById(notExistTripId)).isFalse();
        }

    }

    @Nested
    class 사용자_여행_통계_조회{
        @Test
        void 총_여행_개수와_종료된_여행_개수를_반환한다(){
            // given
            Long tripperId = setupTripperId();
            LocalDate today = LocalDate.of(2023, 4, 28);
            Trip terminatedTrip1 = TripFixture.decided_nullId(tripperId, today.minusDays(3), today.minusDays(1));
            Trip terminatedTrip2 = TripFixture.decided_nullId(tripperId, today.minusDays(3), today.minusDays(1));
            Trip terminatedTrip3 = TripFixture.decided_nullId(tripperId, today.minusDays(3), today.minusDays(1));
            Trip unTerminatedTrip1 = TripFixture.decided_nullId(tripperId, today.plusDays(1), today.plusDays(3));
            Trip unTerminatedTrip2 = TripFixture.decided_nullId(tripperId, today.plusDays(1), today.plusDays(3));

            em.persist(terminatedTrip1);
            em.persist(terminatedTrip2);
            em.persist(terminatedTrip3);
            em.persist(unTerminatedTrip1);
            em.persist(unTerminatedTrip2);


            // when
            TripStatistics tripStatistics = tripQueryRepository.findTripStaticsByTripperId(tripperId, today);

            // then
            assertThat(tripStatistics.getTerminatedTripCnt()).isEqualTo(3);
            assertThat(tripStatistics.getTotalTripCnt()).isEqualTo(5);
        }
    }

    private Long setupTripperId() {
        User user = UserFixture.googleUser_NullId();
        em.persist(user);
        return user.getId();
    }

}
