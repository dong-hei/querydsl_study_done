package study.querydslstudy.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import study.querydslstudy.dto.MemberSearchCondition;
import study.querydslstudy.dto.MemberTeamDto;
import study.querydslstudy.dto.QMemberTeamDto;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydslstudy.entity.QMember.member;
import static study.querydslstudy.entity.QTeam.team;

/**
 * 공통성이 없고 특정 API에 종속되어있는
 * 즉 쿼리가 특화되어있는 경우는 이런식으로 따로 Repo를 만드는 것이 좋다.
 */
@Repository

public class MemberQueryRepository {

    private final JPAQueryFactory qf;

    public MemberQueryRepository(EntityManager em, JPAQueryFactory qf) {
        this.qf = new JPAQueryFactory(em);
    }


    //where절 파람 사용
    public List<MemberTeamDto> search(MemberSearchCondition condition) {

        return qf
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                // 이걸 .selectFrom(member) 로 바꿔도 사용가능하다.=> 재사용성이 좋다.
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null? member.age.loe(ageLoe) : null;
    }

}
