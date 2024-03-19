package study.querydslstudy.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydslstudy.dto.MemberSearchCondition;
import study.querydslstudy.dto.MemberTeamDto;
import study.querydslstudy.dto.QMemberTeamDto;
import study.querydslstudy.entity.Member;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydslstudy.entity.QMember.member;
import static study.querydslstudy.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom  {

    private final JPAQueryFactory qf;

    public MemberRepositoryImpl(EntityManager em, JPAQueryFactory qf) {
        this.qf = new JPAQueryFactory(em);
    }

    @Override
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

    /**
     * 페이징 처리
     * @param condition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = qf
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
                .offset(pageable.getOffset())  // Paging 1, 몇번째를 스킵하고 몇번째부터 시작할것인가
                .limit(pageable.getPageSize()) // Paging 2, 한 페이지에 몇개까지 조회할것인가
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = qf
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
                .offset(pageable.getOffset())  // Paging 1
                .limit(pageable.getPageSize()) // Paging 2
                .fetch();

        long total = qf
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                ).fetchCount(); // 내가 직접 totalCount 쿼리를 날린다.
//        이렇게 나누면 함수 자체에 조건을 부여할수있다.

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplexOptimize(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = qf
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
                .offset(pageable.getOffset())  // Paging 1
                .limit(pageable.getPageSize()) // Paging 2
                .fetch();

         JPAQuery<Member>  countQuery = qf
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
//        countQuery 최적화 - 함수 자체에 조건을 부여해서 성능 최적화
//        조건부가 만족할때만 쿼리를 날려준다.
//        (첫번째 페이지가 100개인데 데이터가 3개밖에 안나오면 CountQuery를 사용하지 않고 그걸 totalCount로 한다)
        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount());    }
}
