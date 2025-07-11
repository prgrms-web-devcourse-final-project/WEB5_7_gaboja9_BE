// package io.gaboja9.mockstock.domain.trades.repository;
//
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.gaboja9.mockstock.domain.members.entity.Members;
// import io.gaboja9.mockstock.domain.trades.entity.TradeType;
// import io.gaboja9.mockstock.domain.trades.entity.Trades;
//
// import jakarta.persistence.EntityManager;
//
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.List;
//
// @DataJpaTest
// class TradesRepositoryTest {
//
//    @Autowired private TradesRepository tradesRepository;
//
//    @Autowired private EntityManager em;
//
//    @Test
//    @DisplayName("기간 내 거래만 조회되는지 확인")
//    void findByStockCodeOrStockNameAndCreatedAtBetween() {
//        // given
//        Members member =
//                new Members(
//                        null,
//                        "test@example.com",
//                        "testUser",
//                        "google",
//                        "test.png",
//                        5000,
//                        0,
//                        LocalDateTime.now());
//        em.persist(member);
//        em.flush();
//
//        Long memberId = member.getId();
//
//        // 거래 - 기간 외
//        Trades trade1 = new Trades("005930", "삼성전자", TradeType.BUY, 10, 70000, member);
//        trade1.setCreatedAt(LocalDateTime.of(2025, 6, 25, 10, 0));
//        em.persist(trade1);
//
//        // 거래 - 기간 내
//        Trades trade2 = new Trades("005930", "삼성전자", TradeType.SELL, 5, 80000, member);
//        trade2.setCreatedAt(LocalDateTime.of(2025, 7, 3, 10, 0));
//        em.persist(trade2);
//
//        em.flush();
//        em.clear();
//
//        // when
//        LocalDateTime start = LocalDate.of(2025, 7, 1).atStartOfDay();
//        LocalDateTime end = LocalDate.of(2025, 7, 8).atTime(23, 59, 59);
//
//        List<Trades> result =
//                tradesRepository.findByStockCodeOrStockNameAndCreatedAtBetween(
//                        "005930", "삼성전자", start, end, memberId);
//
//        // then
//        assertThat(result).hasSize(1);
//        assertThat(result.get(0).getTradeType()).isEqualTo(TradeType.SELL);
//        assertThat(result.get(0).getCreatedAt()).isEqualTo(trade2.getCreatedAt());
//    }
// }
