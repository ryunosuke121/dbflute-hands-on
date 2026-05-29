package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.dbflute.exception.NonSpecifiedColumnAccessException;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberSecurityBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberSecurity;
import org.docksidestage.handson.dbflute.exentity.MemberStatus;
import org.docksidestage.handson.dbflute.exentity.Product;
import org.docksidestage.handson.dbflute.exentity.Purchase;
import org.docksidestage.handson.unit.UnitContainerTestCase;

public class HandsOn03Test extends UnitContainerTestCase {
    @Resource
    private MemberBhv memberBhv;

    @Resource
    private MemberSecurityBhv memberSecurityBhv;

    @Resource
    private PurchaseBhv purchaseBhv;

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

    public void test_会員ステータスの表示順カラムで会員を並べて検索() throws Exception {
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Desc();
        });
        log(memberList);

        // ## Assert ##
        assertTrue(!memberList.isEmpty());

        // 会員ステータスのデータ自体を取得していないこと
        for (Member member : memberList) {
            assertFalse(member.getMemberStatus().isPresent());
        }

        // 会員が会員ステータスごとに固まって並んでいること (順序は問わない)
        Set<String> arrivedStatusCodes = new HashSet<>();
        String prevStatusCode = null;
        for (Member member : memberList) {
            String statusCode = member.getMemberStatusCode();
            if (!statusCode.equals(prevStatusCode)) {
                // 新しいステータスのグループに切り替わった。既出ならばらけている
                assertFalse(arrivedStatusCodes.contains(statusCode));
                arrivedStatusCodes.add(statusCode);
            }
            prevStatusCode = statusCode;
        }
    }

    public void test_生年月日が存在する会員の購入を検索() throws Exception {
        // ## Act ##
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb -> {
            // 会員名称・会員ステータス名称・商品名をログ出力するために取得する
            cb.setupSelect_Member().withMemberStatus();
            cb.setupSelect_Product();
            // 生年月日が存在する会員の購入に絞り込む
            cb.query().queryMember().setBirthdate_IsNotNull();
            // 購入日時の降順、購入価格の降順、商品IDの昇順、会員IDの昇順
            cb.query().addOrderBy_PurchaseDatetime_Desc();
            cb.query().addOrderBy_PurchasePrice_Desc();
            cb.query().queryProduct().addOrderBy_ProductId_Asc();
            cb.query().queryMember().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember().get();
            MemberStatus memberStatus = member.getMemberStatus().get();
            Product product = purchase.getProduct().get();

            // 発行されたクエリ
            // select dfloc.PURCHASE_ID as PURCHASE_ID, dfloc.MEMBER_ID as MEMBER_ID, dfloc.PRODUCT_ID as PRODUCT_ID, dfloc.PURCHASE_DATETIME as PURCHASE_DATETIME, dfloc.PURCHASE_COUNT as PURCHASE_COUNT, dfloc.PURCHASE_PRICE as PURCHASE_PRICE, dfloc.PAYMENT_COMPLETE_FLG as PAYMENT_COMPLETE_FLG, dfloc.REGISTER_DATETIME as REGISTER_DATETIME, dfloc.REGISTER_USER as REGISTER_USER, dfloc.UPDATE_DATETIME as UPDATE_DATETIME, dfloc.UPDATE_USER as UPDATE_USER, dfloc.VERSION_NO as VERSION_NO
            // , dfrel_0.MEMBER_ID as MEMBER_ID_0, dfrel_0.MEMBER_NAME as MEMBER_NAME_0, dfrel_0.MEMBER_ACCOUNT as MEMBER_ACCOUNT_0, dfrel_0.MEMBER_STATUS_CODE as MEMBER_STATUS_CODE_0, dfrel_0.FORMALIZED_DATETIME as FORMALIZED_DATETIME_0, dfrel_0.BIRTHDATE as BIRTHDATE_0, dfrel_0.REGISTER_DATETIME as REGISTER_DATETIME_0, dfrel_0.REGISTER_USER as REGISTER_USER_0, dfrel_0.UPDATE_DATETIME as UPDATE_DATETIME_0, dfrel_0.UPDATE_USER as UPDATE_USER_0, dfrel_0.VERSION_NO as VERSION_NO_0
            // , dfrel_0_0.MEMBER_STATUS_CODE as MEMBER_STATUS_CODE_0_0, dfrel_0_0.MEMBER_STATUS_NAME as MEMBER_STATUS_NAME_0_0, dfrel_0_0.DESCRIPTION as DESCRIPTION_0_0, dfrel_0_0.DISPLAY_ORDER as DISPLAY_ORDER_0_0
            //                    , dfrel_1.PRODUCT_ID as PRODUCT_ID_1, dfrel_1.PRODUCT_NAME as PRODUCT_NAME_1, dfrel_1.PRODUCT_HANDLE_CODE as PRODUCT_HANDLE_CODE_1, dfrel_1.PRODUCT_CATEGORY_CODE as PRODUCT_CATEGORY_CODE_1, dfrel_1.PRODUCT_STATUS_CODE as PRODUCT_STATUS_CODE_1, dfrel_1.REGULAR_PRICE as REGULAR_PRICE_1, dfrel_1.REGISTER_DATETIME as REGISTER_DATETIME_1, dfrel_1.REGISTER_USER as REGISTER_USER_1, dfrel_1.UPDATE_DATETIME as UPDATE_DATETIME_1, dfrel_1.UPDATE_USER as UPDATE_USER_1, dfrel_1.VERSION_NO as VERSION_NO_1
            // from purchase dfloc
            // inner join member dfrel_0 on dfloc.MEMBER_ID = dfrel_0.MEMBER_ID
            // inner join member_status dfrel_0_0 on dfrel_0.MEMBER_STATUS_CODE = dfrel_0_0.MEMBER_STATUS_CODE
            // inner join product dfrel_1 on dfloc.PRODUCT_ID = dfrel_1.PRODUCT_ID
            // where dfrel_0.BIRTHDATE is not null
            // order by dfloc.PURCHASE_DATETIME desc, dfloc.PURCHASE_PRICE desc, dfrel_1.PRODUCT_ID asc, dfrel_0.MEMBER_ID asc

            // 会員名称・会員ステータス名称・商品名をログ出力
            log("memberName", member.getMemberName(), "memberStatusName", memberStatus.getMemberStatusName(),
                    "productName", product.getProductName());

            // 購入に紐づく会員の生年月日が存在すること
            assertNotNull(member.getBirthdate());
        }
    }

    public void test_2005年10月の1日から3日までに正式会員になった会員を検索() throws Exception {
        // ## Arrange ##
        String fromDateExp = "2005/10/01";
        String toDateExp = "2005/10/03";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        // 正式会員日時(FORMALIZED_DATETIME)はLocalDateTimeなので、日付の始まりに変換する
        LocalDateTime fromDatetime = LocalDate.parse(fromDateExp, formatter).atStartOfDay();
        LocalDateTime toDatetime = LocalDate.parse(toDateExp, formatter).atStartOfDay();

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            // 会員ステータスも取得するが、会員ステータス名称だけ取れればよい (説明や表示順は不要)
            cb.setupSelect_MemberStatus();
            cb.specify().specifyMemberStatus().columnMemberStatusName();
            // 2005/10/01から2005/10/03までに正式会員になった会員
            cb.query().setFormalizedDatetime_FromTo(fromDatetime, toDatetime, op -> op.compareAsDate());
            // 会員名称に "vi" を含む会員
            cb.query().setMemberName_LikeSearch("vi", op -> op.likeContain());
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            MemberStatus memberStatus = member.getMemberStatus().get();

            // 会員名称・正式会員日時・会員ステータス名称をログ出力
            log("memberName", member.getMemberName(), "formalizedDatetime", member.getFormalizedDatetime(),
                    "memberStatusName", memberStatus.getMemberStatusName());

            // 会員名称に "vi" を含むこと
            assertContains(member.getMemberName(), "vi");

            // 会員ステータスはコードと名称だけが取得されていること
            assertNotNull(memberStatus.getMemberStatusCode());
            assertNotNull(memberStatus.getMemberStatusName());
            assertException(NonSpecifiedColumnAccessException.class, () -> memberStatus.getDescription());
            assertException(NonSpecifiedColumnAccessException.class, () -> memberStatus.getDisplayOrder());

            // 正式会員日時が指定された条件の範囲内であること
            LocalDate formalizedDate = member.getFormalizedDatetime().toLocalDate();
            assertTrue(formalizedDate.isAfter(fromDatetime.toLocalDate()) || formalizedDate.isEqual(fromDatetime.toLocalDate()));
            assertTrue(formalizedDate.isBefore(toDatetime.toLocalDate()) || formalizedDate.isEqual(toDatetime.toLocalDate()));
        }
    }
}
