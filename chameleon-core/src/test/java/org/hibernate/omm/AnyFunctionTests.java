package org.hibernate.omm;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@MongoIntegrationTest
class AnyFunctionTests {

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    @Test
    void test() {
        final var book1 = new Book();
        book1.id = 1L;
        book1.tags = List.of("romance", "french");
        final var book2 = new Book();
        book2.id = 2L;
        book2.tags = List.of("classic", "romance");
        sessionFactory.inTransaction(session -> {
            session.persist(book1);
            session.persist(book2);
        });
        sessionFactory.inTransaction(session -> {
            final var classics = session.createSelectionQuery("from Book where array_contains(tags, :tag)", Book.class)
                    .setParameter("tag", "classic")
                    .getResultList();
            assertThat(classics).singleElement().satisfies(book ->
                    assertThat(book).usingRecursiveComparison().isEqualTo(book2)
            );
        });
    }

    @Entity(name = "Book")
    static class Book {
        @Id
        long id;

        List<String> tags;
    }
}
