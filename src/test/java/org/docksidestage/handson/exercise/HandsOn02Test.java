package org.docksidestage.handson.exercise;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.dbflute.optional.OptionalEntity;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.unit.UnitContainerTestCase;

// TODO itoryu 一応、ここでもJavaDocお願いします (authorだけでもいいので) by jflute (2026/04/26)
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
        	// TODO itoryu わりと定型なので、Lambda引数名は op -> op.likePrefix() みたいでOK by jflute (2026/04/26)
            cb.query().setMemberName_LikeSearch("S", likeSearchOption -> likeSearchOption.likePrefix());
            cb.query().addOrderBy_MemberName_Asc();
        });

        // ## Assert ##
        // 取得したmemberListが空でないこと
        assertTrue(!memberList.isEmpty());

        // 取得したmemberListが条件を満たしていること
        String prevMemberName = null;
        for (Member member : memberList) {
            String memberName = member.getMemberName();

            // TODO itoryu 些細なとこだけど、memberName.startsWith()というメソッドもある by jflute (2026/04/26)
            assertTrue(memberName.charAt(0) == 'S');
            if (prevMemberName != null) {
                assertTrue(memberName.compareTo(prevMemberName) >= 0);
            }
            prevMemberName = memberName;
        }
        // #1on1: こんなメソッドもある。 (2026/04/28)
        assertOrder(memberList, orderBy -> {
        	orderBy.asc(mb -> mb.getMemberName());
		});
    }

    public void test_会員IDが1の会員を検索() throws Exception {
        // ## Act ##
    	// #1on1: 昔のJava6版のDBFluteだと、nullを戻すメソッド (2026/04/28)
		//Member member = memberBhv.selectEntity(cb -> {
		//    cb.query().setMemberId_Equal(1);
		//});
    	// 一方で、なければ例外を投げるメソッド (これは今もある)
		//Member member = memberBhv.selectEntityWithDeletedCheck(cb -> {
		//	cb.query().setMemberId_Equal(1);
		//});
    	
        OptionalEntity<Member> optMember = memberBhv.selectEntity(cb -> {
            cb.query().setMemberId_Equal(1);
        });

        // ## Assert ##
        
        assertTrue(optMember.isPresent());
        
        // done jflute 1on1にてDBFluteのOptionalのお話 (2026/04/26)
        
        // #1on1: DBFluteのOptionalだと、なかった場合はget()でもフレームワーク提供のリッチな例外メッセージ (2026/04/28)
        //Member mondouMember = member.get(); // 問答無用get()
        // #1on1: フレームワークのエラーメッセージ大事だよ話 (2026/04/28)

        // 問答無用get()の代わり、alwaysPresent(), なかったらリッチな例外。
        // ifPresent() と似ているが、なかったときの挙動が違う。
        optMember.alwaysPresent(member -> {
        	assertEquals(1, member.getMemberId());
        });
    }

    public void test_生年月日がない会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setBirthdate_IsNull();
            cb.query().addOrderBy_UpdateDatetime_Desc();
        });

        // ## Assert ##
        // TODO itoryu 0のとき、greenになってしまう by jflute (2026/04/28)

        // allMatch()のJavaDocを見ると...
        // if the stream is empty then true is returned and the predicate is not evaluated.
        assertTrue(memberList.stream().allMatch(member -> member.getBirthdate() == null));

        // ちなみに、anyMatch だと false
        // If the stream is empty then false is returned and the predicate is not evaluated.
        //assertTrue(memberList.stream().anyMatch(member -> member.getBirthdate() == null));
    }
}
