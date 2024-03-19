package study.querydslstudy.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydslstudy.dto.MemberSearchCondition;
import study.querydslstudy.dto.MemberTeamDto;
import study.querydslstudy.dto.QMemberTeamDto;
import study.querydslstudy.entity.Member;


import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydslstudy.entity.QMember.member;
import static study.querydslstudy.entity.QTeam.team;

/**
 * 순수 JPA 리포지토리 와 Querydsl
 */
@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory qf;

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    //JPA
//    public List<Member> findAll(){
//        return em.createQuery("select m from member m", Member.class).getResultList();
//    }


    //QueryDsl
    public List<Member> findAllQuerydsl(){
        return qf
                .selectFrom(member)
                .fetch();
    }

    //JPA
//    public List<Member> findByUserName(String username) {
//        return em.createQuery("select m from Member m where m.username = :username", Member.class)
//                .setParameter("username", username)
//                .getResultList();
//    }

    //QueryDsl
    public List<Member> findByUserNameQuerydsl(String username){
        return qf
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    //Builder 사용
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) { // null일수도있고 ""일수도 있으니까 StringUtils.hasText() 사용
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return qf
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
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
