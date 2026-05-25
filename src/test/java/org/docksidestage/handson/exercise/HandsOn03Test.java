package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberSecurityBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberSecurity;
import org.docksidestage.handson.dbflute.exentity.MemberStatus;
import org.docksidestage.handson.unit.UnitContainerTestCase;

public class HandsOn03Test extends UnitContainerTestCase {
    @Resource
    private MemberBhv memberBhv;

    @Resource
    private MemberSecurityBhv memberSecurityBhv;

    public void test_会員名称がSで始まる1968年1月1日以前に生まれた会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setMemberName_LikeSearch("S", likeSearchOption -> likeSearchOption.likePrefix());
            cb.query().setBirthdate_LessEqual(LocalDate.of(1968, 1, 1));
            cb.query().addOrderBy_Birthdate_Asc();
        });
        log(memberList);

        // ## Assert ##
        // done itoryu assertHasAnyElement(notEmptyList); という専用メソッドがあるのでぜひ使ってください by jflute (2026/04/26)
        assertHasAnyElement(memberList);

        
        // #1on1: こっちのallMatch()は安全 (2026/04/28)
        assertTrue(memberList.stream().allMatch(member -> member.getMemberName().charAt(0) == 'S'));

        // done itoryu ちょっと見栄え的に、Lambda式の中をprivateメソッドにするとか何か見た目工夫したいところ by jflute (2026/04/26)
        // #1on1: 書いてる時はだーっと書くのは全然アリでぼくもそうしてます。その後、最後の仕上げで見た目を考えるフェーズがある。 (2026/04/28)
        // そして、リファクタリング機能を指に馴染ませているので、パパッと整える。
        // ここだと特に、レビューワーは isEqual or isBefore の判定の構造を見たい。
        // そのロジカルな行のノイズを減らして、できるだけロジック構造だけの行にしたい。
        LocalDate baseDate = LocalDate.of(1968, 1, 1);
        assertTrue(memberList.stream().allMatch(member -> isBornOnOrBefore(member, baseDate)));
    }

    private boolean isBornOnOrBefore(Member member, LocalDate baseDate) {
        LocalDate birthdate = member.getBirthdate();
        return birthdate.isEqual(baseDate) || birthdate.isBefore(baseDate);
    }

    // #1on1: 会員ステータスと言った場合、テーブルを指すのか？カラムを指すのか？ (2026/04/28)
    // 会員ステータスコードという名前の付け方と会員ステータス(カラム)という名前の付け方。
    public void test_会員ステータスと会員セキュリティ情報も取得して会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
        	// TODO itoryu 会員ステータスを取得していない by jflute (2026/04/28)
        	// TODO itoryu 実装順序は、データの取得、絞り込み、並び替え by jflute (2026/04/26)
        	//  => http://dbflute.seasar.org/ja/manual/function/ormapper/conditionbean/effective.html#implorder
            cb.setupSelect_MemberStatus();
            cb.setupSelect_MemberSecurityAsOne();
            cb.query().addOrderBy_Birthdate_Desc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertTrue(!memberList.isEmpty());

        // TODO jflute 1on1にて、カージナリティのお話 (2026/04/26)
        // #1on1: まず、カージナリティという言葉の使われる箇所が二箇所あって... (2026/04/28)
        // 1. テーブル間のカージナリティ // ここで話すのはこっち
        //    → 会員1人につき、購入はいくつ？ステータスはいくつ？セキュリティ情報はいくつ？ (2026/05/12)
        //      購入はいくつ？ => n (複数ありえる)
        //      ステータスはいくつ？ => 1
        //      セキュリティは情報いくつ？ => 1
        //      one-to-many, 1:n 1n 1:* 1*
        //
        //      会員 - 会員セキュリティ情報 =>  1 : 1       // 必ず存在する1
        //      会員 - 会員退会情報 =>         1 : 0..1   // いないかもしれない1
        //
        // 2. カラムのカージナリティ // itoryuさんのはこっち
        //    → カラムの値の種類が少ないと、インデックスの効果が低い (Bツリーのお話)
        //
        // #1on1: ERDのリレーションシップの記法 (2026/05/12)
        // 3種類とかある。ハンズオンだと3本脚のやつを使っている。
        //
        // #1on1: ERDのお話 (2026/05/12)
        // 現場のERD。
        // ERDでDBを思考する話。
        //
        // #1on1: OSSの活動のお話 (2026/05/12)
        Member prevMember = null;
        for (Member member : memberList) {
            MemberStatus memberStatus = member.getMemberStatus().get();
            assertTrue(memberStatus != null);

            MemberSecurity memberSecurity = member.getMemberSecurityAsOne().get();
            assertTrue(memberSecurity != null);

            if (prevMember != null) {
            	// done itoryu カラム名がbirthdateなので、birthdayじゃなくてbirthdate by jflute (2026/04/28)
            	// この場合、どっちが合ってるとか好きとかじゃなくて、カラム名でそうなってるということは決めの問題なので、
            	// カラムに合わせましょう。
                LocalDate memberBirthdate = member.getBirthdate();
                LocalDate prevMemberBirthdate = prevMember.getBirthdate();
                if (memberBirthdate != null && prevMemberBirthdate != null) {
                    // 生年月日が降順であること
                    assertTrue(memberBirthdate.isBefore(prevMemberBirthdate));
                } else if (memberBirthdate == null && prevMemberBirthdate == null) {
                    // 生年月日が設定されていないmember同士はidが昇順であること
                    assertTrue(member.getMemberId() > prevMember.getMemberId());
                }
            }

            prevMember = member;
        }
    }

    public void test_会員セキュリティ情報のリマインダ質問で2という文字が含まれている会員を検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberSecurityAsOne().setReminderQuestion_LikeSearch("2", op -> op.likeContain());
            cb.query().addOrderBy_Birthdate_Desc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertTrue(!memberList.isEmpty());

        List<Integer> memberIds = memberList.extractColumnList(member -> member.getMemberId());
        ListResultBean<MemberSecurity> memberSecurities = memberSecurityBhv.selectList(cb -> {
            cb.query().setMemberId_InScope(memberIds);
        });
        Map<Integer, String> memberSecurityMap = memberSecurities.stream()
            .collect(Collectors.toMap(MemberSecurity::getMemberId, MemberSecurity::getReminderQuestion));

        for(Member member: memberList) {
            String reminderQuestion = memberSecurityMap.get(member.getMemberId());
            assertTrue(reminderQuestion.contains("2"));

            log("memberName", member.getMemberName(), "reminderQuestion",  reminderQuestion, "\n");
        }
    }
}
