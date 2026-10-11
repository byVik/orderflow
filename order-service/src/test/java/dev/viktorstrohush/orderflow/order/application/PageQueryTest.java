package dev.viktorstrohush.orderflow.order.application;

import dev.viktorstrohush.orderflow.order.application.port.PageQuery;
import dev.viktorstrohush.orderflow.order.application.port.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PageQueryTest {

    @Test
    void acceptsTheFirstPageAndTheLargestSize() {
        PageQuery query = new PageQuery(0, PageQuery.MAX_SIZE);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(50);
    }

    @Test
    void rejectsANegativePage() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PageQuery(-1, 10));
    }

    @Test
    void rejectsASizeOutOfRange() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PageQuery(0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1));
    }

    @Test
    void mappingAPageKeepsItsPosition() {
        PageResult<Integer> page = new PageResult<>(List.of(1, 2), 3, 2, true);

        assertThat(page.map(n -> "#" + n)).isEqualTo(new PageResult<>(List.of("#1", "#2"), 3, 2, true));
    }
}
