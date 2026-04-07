package org.docksidestage.handson.exercise;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
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
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setMemberName_LikeSearch("S%", op -> op.likeContain());
            cb.query().addOrderBy_MemberName_Asc();
        });

        assertTrue(!memberList.isEmpty());
    }
}
