package study.querydslstudy;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydslstudy.dto.MemberDto;
import study.querydslstudy.dto.QMemberDto;
import study.querydslstudy.dto.UserDto;
import study.querydslstudy.entity.Member;
import study.querydslstudy.entity.QMember;
import study.querydslstudy.entity.QTeam;
import study.querydslstudy.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydslstudy.entity.QMember.*;
import static study.querydslstudy.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory qf;

    @BeforeEach
    public void before(){
        qf = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member m1 = new Member("m1", 15, teamA);
        Member m2 = new Member("m2", 21, teamA);

        Member m3 = new Member("m3", 52, teamB);
        Member m4 = new Member("m4", 62, teamB);

        em.persist(m1);
        em.persist(m2);
        em.persist(m3);
        em.persist(m4);
    }

    /**
     * JPQL과 Querydsl의 차이 및 Q타입 활용 방법
     */
    @Test
    public void startJPQL(){
        //m1을 찾아라
        Member findMemberByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "m1")
                .getSingleResult();

        assertThat(findMemberByJPQL.getUsername()).isEqualTo("m1");

        //JPQL 쿼리를 잘못입력했을때 런타임때 오류를 확인한다.
    }

    @Test
    public void startQuerydsl(){
//        JPAQueryFactory qf = new JPAQueryFactory(em); // (필드로 올려서 사용하는것도 가능.)

        /**
         * Q타입 인스턴스 사용방법 1
         */
//        QMember m = new QMember("m"); //new QMember(어떤 q 멤버인지 구분하는 것) 

        /**
         * Q타입 인스턴스 사용방법 2
         */
//          QMember m = member;

            Member findMemberByQuerydsl = qf
                /**
                 * Q타입 인스턴스 사용방법 3 - QMember.member -> 스태틱 인스턴스 사용해서 호출 (가장 편하고 자주 사용한다.)
                 */
                        .select(member)
                        .from(member)
                        .where(member.username.eq("m1")) // eq()를 사용하면 자동으로 파람 바인딩 처리
                        .fetchOne();

        assertThat(findMemberByQuerydsl.getUsername()).isEqualTo("m1");

        //장점 1 : Querydsl 쿼리를 잘못입력했을때 컴파일때 오류를 확인 할수 있다.
        //장점 2 : 파람 바인딩 자리에 eq, like 등 많은 문법을 사용하기 쉽다.
    }

    /**
     * 검색조건 Querydsl 문법
     */
    @Test 
    public void search(){
        Member findMember = qf
                .selectFrom(member) // select from이 같으면 합쳐도 된다.
                .where(member.username.eq("m1")
                        .and(member.age.between(14, 60)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("m1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = qf
                .selectFrom(member) // select from이 같으면 합쳐도 된다.
                .where(member.username.eq("m1"),
//                        ,로 and를 생략할수도 있다, 중간에 null 이 들어가면 null 을 무시한다 (동적쿼리 짤때 유용)
                        (member.age.eq(15)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("m1");
    }

    /**
     * 결과 조회
     */

    @Test
    public void resultFetch(){

//        List<Member> fetch = qf
//                .selectFrom(member)
//                .fetch(); //List로 조회
//
//        Member fetchOne = qf
//                .selectFrom(member)
//                .fetchOne(); //한건 조회
//
//        Member fetchFirst = qf
//                .selectFrom(member)
//                .fetchFirst(); //처음 한건 조회

        QueryResults<Member> result = qf
                .selectFrom(member)
                .fetchResults(); //페이징 시 사용

        //result로 가져올수있는 메소드
        result.getTotal(); 
        result.getOffset(); // 오프셋도 가져올수 있다.
        List<Member> content = result.getResults(); // 페이징시 사용

//        long total = qf
//                .selectFrom(member)
//                .fetchCount(); // 카운트 쿼리만 가져온다.

    }

    /**
     * 회원 정렬 순서
     * 1.회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("m7", 100));
        em.persist(new Member("m8", 100));

        List<Member> result = qf
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) // nullFirst()도 있다.
                .fetch();

        Member m7 = result.get(0);
        Member m8 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(m7.getUsername()).isEqualTo("m7");
        assertThat(m8.getUsername()).isEqualTo("m8");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * 페이징 처리
     */

    @Test
    public void paging1(){
        List<Member> result = qf
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    //
    @Test
    public void paging2(){
        QueryResults<Member> queryResults = qf
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //앞에 몇개를 스킵할것인지
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults()).size().isEqualTo(2);
    }

    /**
     * 집합 1
     */

    @Test
    public void aggregation(){
        //Tuple = Querydsl에서 제공하는 Tuple (여러개의 타입에서 꺼낼수 있는것)
        List<Tuple> result = qf
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(150);
        assertThat(tuple.get(member.age.avg())).isEqualTo(37.5);
        assertThat(tuple.get(member.age.max())).isEqualTo(62);
        assertThat(tuple.get(member.age.min())).isEqualTo(15);
    }

    /**
     *  집합 2 - 팀 명과 각 팀의 평균 연령을 구하라.
     */
    @Test
    public void grouping() throws Exception {
        List<Tuple> result = qf
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(18);


        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(57);

        //sql처럼 having도 넣을 수 있다.
    }

    /**
     * 조인 1 - 팀 A에 소속된 모든 회원
     */
    @Test
    public void join(){
        List<Member> result = qf
                .selectFrom(member)
                .join(member.team, team) //left right 도 가능
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("m1", "m2");
    }

    /**
     * 조인 2 - 세타조인 :회원의 이름이 팀 이름과 같은 회원 조회 (연관관계가 없어도 가능한 조인)
     */
    @Test
    public void thetaJoin(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        List<Member> result = qf
                .select(member)
                .from(member, team) //  from 절의 2개를 나열하는게 세타 조인
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
        // 모든 Member 테이블, Team 테이블 다 조인하고 (from 절에 여러 엔티티를 선택해 세타 조인)
        // Member, Team 이름이 같은 결과를 조사한다.
    }

    /**
     * 조인 3 - ON절을 활용한 조인
     * 1. 조인 대상 필터링
     * 회원과 팀을 조인하면서 팀 이름이 teamA 만 조인, 회원은 모두 조회
     * JPQL : select m , from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void joinOnFiltering(){
        List<Tuple> result = qf
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
//                .leftJoin(member.team, team)
//                .on(team.name.eq("teamA"))
// inner 조인과 기능이 비슷하기 때문에 이런 케 이스는 익숙한 where 절을 사용하고
// 외부조인이 필요한 경우에만 on절을 사용하자.
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     *  조인 3 - ON절을 활용한 조인
     * 2. 연관관계가 없는 엔티티 외부 조인
     * (회원의 이름이 팀 이름과 같은 대상 외부 조인)
     */
    @Test
    public void joinOnNoRelation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = qf
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                //보통은 member.team, member인데 team만 쓰면 (즉 id로 매칭을 안하면) on()절로 매칭을 한다.
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 페치조인 - SQL 조인을 활용해 연관된 엔티티를 SQL 한번에 조회하는 기능, 주로 성능 최적화에 사용하는 방법
     */

    @PersistenceUnit
    EntityManagerFactory enf;
    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = qf
                .selectFrom(member)
                .where(member.username.eq("m1"))
                .fetchOne();

        boolean loaded = enf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 로딩된 엔티티인지 아닌지 확인용
        assertThat(loaded).as("Fetch Join is not applied").isFalse();
    }

    @Test
    public void fetchJoin(){
        em.flush();
        em.clear();

        Member findMember = qf
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("m1"))
                .fetchOne();

        boolean loaded = enf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("Fetch Join is not allow").isTrue();
    }

    /**
     * 서브쿼리 1 - 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery(){

        QMember memberSub = new QMember("memberSub"); // 바깥에 있는 쿼리와 겹치면 안됨

        List<Member> result = qf
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(62);
    }   
    
    /**
     * 서브쿼리 2 - 나이가 평균이상인 회원 조회
     */
    @Test
    public void subQueryGoe(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = qf
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(52,62);
    }

    /**
     * 서브쿼리 3 - 나이가 초과인 경우
     */
    @Test
    public void subQueryIn(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = qf
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(20)) // 20살 초과
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(21,52,62);
    } 
    
    /**
     * 서브쿼리 4 - 셀렉트 서브쿼리
     */
    @Test
    public void selectSubQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = qf
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                        .from(member)
                        .fetch();

        for (Tuple Tuple : result) {
            System.out.println("Tuple = " + Tuple);
        }
        //JPA는 from 절의 서브쿼리가 불가능 하다. 당연히 Querydsl도 안된다.
        //=> 해결 방안 : 1. 서브 쿼리를 join으로 변경 (가능한 상황이 있을때 사용)
        //           , 2. 애플리케이션에서 쿼리를 2번 분리해서 사용 (엄청 복잡하지 않은 이상 사용)
        //           , 3. nativeSQL 사용
    }

    /**
     * Case문 - 가급적이면 DB에 쓰지말고 Application Logic에서 해결하자.
     *
     */
    
    @Test
    public void baseCase() throws Exception {
        List<String> result = qf
                .select(member.age
                        .when(15).then("청소년")
                        .when(21).then("젊은이")
                        .otherwise("틀딱"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void complexCase() throws Exception {
        List<String> result = qf
                .select(new CaseBuilder()
                                .when(member.age.between(0,20)).then("청소년")
                                .when(member.age.between(21,30)).then("청년")
                                .when(member.age.between(31,50)).then("중년")
                                .otherwise("틀딱")
                        )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("연령대 비교 (complexCase) = " + s);
        }

    }

    /**
     * 상수
     */
    @Test
    public void constant(){
        List<Tuple> result = qf
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat(){
        List<String> result = qf
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("m1"))
                .fetch();

        for (String o : result) {
            System.out.println("o = " + o);

        } // enum 처리할때 자주 사용
    }

    /**
     * 프로젝션 (select의 대상 지정) 과 결과 반환
     */

    @Test
    public void simpleProjection() throws Exception {
        List<String> result = qf
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = qf
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        } // 되도록이면 Tuple (querydsl에 종속적, respository에서 쓸때는 괜찮) 보단 Dto로 변환하는것을 추천(은데 외부로 내보낼때)
    }

    /**
     * 프로젝션 과 결과 반환을 dto로 하기
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery("select new study.querydslstudy.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    /**
     * 프로젝션 과 결과 반환을 dto로 하기 1 - setter
     */

    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = qf.
                select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age)) //setter로 데이터를 injection
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 프로젝션 과 결과 반환을 dto로 하기 2 - 필드로 하기
     */
    @Test
    public void findDtoByField() throws Exception {
        //given
        List<MemberDto> result = qf.
                select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age)) //field로 데이터를 바로 injection
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 프로젝션 과 결과 반환을 dto로 하기 3 - 생성자로 하기
     */
    @Test
    public void findDtoByConstructor() throws Exception {
//        QMember memberSub = new QMember("memberSUb"); // 서브쿼리의 이름 지정을 위함
        //given
        List<MemberDto> result = qf.
                select(Projections.constructor(MemberDto.class,
                        member.username,
//                      ExpressionUtils.as(JPAExpressions.select(memberSub.age.max().from(memberSub),"age")) // 서브쿼리의 이름을 지정
                        member.age)) //생성자로 데이터를 바로 injection (해당 클래스에 username,age가 딱 맞아야함)
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() throws Exception {
        //given
        List<UserDto> result = qf.
                select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age)) //생성자로 데이터를 바로 injection (해당 클래스에 username,age가 딱 맞아야함)
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * QueryProjection : 생성자와 달리 컴파일 시점에 확인 가능하다.
     * 단점 1 : Q파일을 생성해야 한다.
     * 단점 2 : MemberDto 자체가 QueryDsl에 의존성을 갖는다.
     * (Dto는 깔끔하게 가져가고싶은데 컨트롤러,리포지토리 등등 여기저기서 참조되면?)
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = qf
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * 동적쿼리 - BooleanBuilder로 해결
     */

    @Test
    public void dynamicQ_BooleanBuilder() throws Exception {
        String usernameParam = "m1";
        Integer ageParam = 25;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);


    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        } // null이면 아예 이 코드가 안타게 한다.
        if (ageCond != null){
            builder.and(member.age.eq(ageCond));
        } // null이면 아예 이 코드가 안타게 한다.

        return qf
                .select(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적쿼리 - where 다중 파람으로 해결
     */

    @Test
    public void dynamicQ_WhereParam() throws Exception {
        String usernameParam = "m1";
        Integer ageParam = 25;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return qf
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond)) // 즉 where안에 값이 null이 되면 null을 무시한다.
//                .where(allEq(usernameCond, ageCond))     //이런식으로 조립을 할수도 있다.
                .fetch();
    }


    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond == null) {
            return null;
        }
        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null; // null 체크는 주의해서 처리
    }

    //이런식으로 조립을 할수도 있다.
    // (조립하기 위해서는 ageEq,usernameEq 리턴을 Predicate가 아닌 BoolenExpression을 받아야한다.)
    private Predicate allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    //ex. 광고상태,날짜 이런식으로 조립할수있다.

    /**
     * 수정 삭제 배치 쿼리 
     * (쿼리 한번으로 대량 데이터 수정)
     */

    @Test
    public void bulkUpdate() throws Exception {

        //m1 15 => m1
        //m2 21 => m2
        //m3 52 => m3
        //m4 62 => m4
        long count = qf
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(25))
                .execute();

        //벌크연산에서 영속성 컨텍스트를 초기화 하지 않으면 발생하는 문제!
        //m1 15 => DB 비회원
        //m2 21 => DB 비회원
        //m3 52 => DB m3 (1,2와 3,4의 상태가 맞지않는다.)
        //m4 62 => DB m4
        //select해서 멤버를 가져왔을때 m1,m2 DB에서 select를 해왔어도 m1,m2 상태가 유지가 된다. (영속성 컨텍스트가 항상 우선권을 가진다.)

        em.flush(); // 영속성 컨텍스트와 DB를 맞춘다.
        em.clear();// 상태가 맞지않는것에 대한 해결책, 벌크 영속성 컨텍스트 초기화

        List<Member> result = qf.selectFrom(member).fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

    }

    @Test
    public void bulkAdd() throws Exception {
        long count = qf
                .update(member)
                .set(member.age, member.age.add(1)) // multiple()도 사용가능
                .execute();
    }

    @Test
    public void bulkDelete() throws Exception {
        long count = qf
                .delete(member)
                .where(member.age.gt(22))
                .execute();
    }

    /**
     * SQL function 호출하기 -
     */

    @Test
    public void sqlFunction() throws Exception {
        List<String> result = qf
                .select(Expressions.stringTemplate( //숫자면 numberTemplate
                        "function('replace', {0}, {1}, {2})",
                        member.username, "m", "MBR")) // m -> MBR로 변환
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() throws Exception {
        List<String> result = qf
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})",
//                                member.username)))
                .where(member.username.eq(member.username.lower())) // 소문자 변환
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}

