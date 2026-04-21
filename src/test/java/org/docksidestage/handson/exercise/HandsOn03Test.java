package org.docksidestage.handson.exercise;

import java.time.LocalDate;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberSecurity;
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
                .allMatch(member -> member.getBirthdate().isEqual(LocalDate.of(1968, 1, 1)) || member.getBirthdate()
                        .isBefore(LocalDate.of(1968, 1, 1))));
    }

    public void test_会員ステータスと会員セキュリティ情報も取得して会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().addOrderBy_Birthdate_Desc();
            cb.query().addOrderBy_MemberId_Asc();
            cb.setupSelect_MemberSecurityAsOne();
        });

        // ## Assert ##
        assertTrue(!memberList.isEmpty());

        Member prevMember = null;
        for (Member member : memberList) {
            MemberSecurity memberSecurity = member.getMemberSecurityAsOne().get();
            assertTrue(memberSecurity != null);
            if (prevMember != null) {
                LocalDate memberBirthday = member.getBirthdate();
                LocalDate prevBirthday = prevMember.getBirthdate();
                if (memberBirthday != null && prevBirthday != null) {
                    // 生年月日が降順であること
                    assertTrue(memberBirthday.isBefore(prevBirthday));
                } else if (memberBirthday == null && prevBirthday == null) {
                    // 生年月日が設定されていないmember同士はidが昇順であること
                    assertTrue(member.getMemberId() > prevMember.getMemberId());
                }
            }

            prevMember = member;
        }
    }
}
