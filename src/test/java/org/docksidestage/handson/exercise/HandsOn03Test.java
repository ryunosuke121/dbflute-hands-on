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
        // TODO itoryu assertHasAnyElement(notEmptyList); という専用メソッドがあるのでぜひ使ってください by jflute (2026/04/26)
        assertTrue(!memberList.isEmpty());
        
        // #1on1: こっちのallMatch()は安全 (2026/04/28)
        assertTrue(memberList.stream().allMatch(member -> member.getMemberName().charAt(0) == 'S'));

        // TODO itoryu ちょっと見栄え的に、Lambda式の中をprivateメソッドにするとか何か見た目工夫したいところ by jflute (2026/04/26)
        // #1on1: 書いてる時はだーっと書くのは全然アリでぼくもそうしてます。その後、最後の仕上げで見た目を考えるフェーズがある。 (2026/04/28)
        // そして、リファクタリング機能を指に馴染ませているので、パパッと整える。
        // ここだと特に、レビューワーは isEqual or isBefore の判定の構造を見たい。
        // そのロジカルな行のノイズを減らして、できるだけロジック構造だけの行にしたい。
        assertTrue(memberList.stream()
                .allMatch(member -> member.getBirthdate().isEqual(LocalDate.of(1968, 1, 1)) || member.getBirthdate()
                        .isBefore(LocalDate.of(1968, 1, 1))));
    }

    // #1on1: 会員ステータスと言った場合、テーブルを指すのか？カラムを指すのか？ (2026/04/28)
    // 会員ステータスコードという名前の付け方と会員ステータス(カラム)という名前の付け方。
    public void test_会員ステータスと会員セキュリティ情報も取得して会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
        	// TODO itoryu 会員ステータスを取得していない by jflute (2026/04/28)
        	// TODO itoryu 実装順序は、データの取得、絞り込み、並び替え by jflute (2026/04/26)
        	//  => http://dbflute.seasar.org/ja/manual/function/ormapper/conditionbean/effective.html#implorder
            cb.query().addOrderBy_Birthdate_Desc();
            cb.query().addOrderBy_MemberId_Asc();
            cb.setupSelect_MemberSecurityAsOne();
        });

        // ## Assert ##
        assertTrue(!memberList.isEmpty());

        // TODO jflute 1on1にて、カージナリティのお話 (2026/04/26)
        Member prevMember = null;
        for (Member member : memberList) {
            MemberSecurity memberSecurity = member.getMemberSecurityAsOne().get();
            assertTrue(memberSecurity != null);
            if (prevMember != null) {
            	// TODO itoryu カラム名がbirthdateなので、birthdayじゃなくてbirthdate by jflute (2026/04/28)
            	// この場合、どっちが合ってるとか好きとかじゃなくて、カラム名でそうなってるということは決めの問題なので、
            	// カラムに合わせましょう。
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
