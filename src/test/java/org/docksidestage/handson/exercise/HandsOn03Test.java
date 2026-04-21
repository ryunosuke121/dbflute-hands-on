package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.unit.UnitContainerTestCase;

public class HandsOn03Test extends UnitContainerTestCase {
    @Resource
    private MemberBhv memberBhv;

    public void test_会員名称がSで始まる1968年1月1日以前に生まれた会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setMemberName_LikeSearch("S", likeSearchOption -> likeSearchOption.likePrefix());
            cb.query().setBirthdate_LessEqual(LocalDate.of(1968, 1, 1));
            cb.query().addOrderBy_Birthdate_Asc();
        });
        log(memberList);

        // ## Assert ##
        assertTrue(!memberList.isEmpty());
        assertTrue(memberList.stream().allMatch(member -> member.getMemberName().charAt(0) == 'S'));
        assertTrue(memberList.stream()
                .allMatch(member -> member.getBirthdate().isEqual(LocalDate.of(1968, 1, 1)) ||
                        member.getBirthdate().isBefore(LocalDate.of(1968, 1, 1))));
    }

    public void test_会員ステータスと会員セキュリティ情報も取得して会員を検索() throws Exception {
        // ## Act ##


        // ## Assert ##
    }
}
