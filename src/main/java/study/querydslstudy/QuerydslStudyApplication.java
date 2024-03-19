package study.querydslstudy;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuerydslStudyApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuerydslStudyApplication.class, args);
	}

	@Bean
	JPAQueryFactory jpaQueryFactory(EntityManager em) {
		return new JPAQueryFactory(em);
	}
	// JPAQueryFactory를 쓸때 이렇게 Bean을 주입하면 생성자를 줄여쓸수있다.
}
