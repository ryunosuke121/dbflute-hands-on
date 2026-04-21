package org.docksidestage.handson.exercise;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.dbflute.optional.OptionalEntity;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.unit.UnitContainerTestCase;

public class HandsOn02Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    public void test_existsTestData() throws Exception {
        // ## Arrange ##

        // ## Act ##
        int count = memberBhv.selectCount(cb -> {});
        boolean result = count > 0;

        // ## Assert ##
        assertTrue(result);
    }

    public void test_会員名称がSで始まる会員を取得() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setMemberName_LikeSearch("S", likeSearchOption -> likeSearchOption.likePrefix());
            cb.query().addOrderBy_MemberName_Asc();
        });

        // ## Assert ##
        // 取得したmemberListが空でないこと
        assertTrue(!memberList.isEmpty());

        // 取得したmemberListが条件を満たしていること
        String prevMemberName = null;
        for(Member member: memberList) {
            String memberName = member.getMemberName();

            assertTrue(memberName.charAt(0) == 'S');
            if (prevMemberName != null) {
                assertTrue(memberName.compareTo(prevMemberName) >= 0);
            }
            prevMemberName = memberName;
        }
    }

    public void test_会員IDが1の会員を検索() throws Exception {
        // ## Act ##
        OptionalEntity<Member> member = memberBhv.selectEntity(cb -> {
           cb.query().setMemberId_Equal(1);
        });

        // ## Assert ##
        assertTrue(member.isPresent());
    }

    public void test_生年月日がない会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setBirthdate_IsNull();
            cb.query().addOrderBy_UpdateDatetime_Desc();
        });

        // ## Assert ##
        assertTrue(memberList.stream().allMatch(member -> member.getBirthdate() == null));
    }
}
