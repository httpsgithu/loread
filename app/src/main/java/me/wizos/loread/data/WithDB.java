package me.wizos.loread.data;

import android.support.v4.util.ArrayMap;
import android.util.SparseArray;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.query.QueryBuilder;
import me.wizos.loread.App;
import me.wizos.loread.bean.Article;
import me.wizos.loread.bean.Feed;
import me.wizos.loread.bean.Img;
import me.wizos.loread.bean.RequestLog;
import me.wizos.loread.bean.Tag;
import me.wizos.loread.data.dao.ArticleDao;
import me.wizos.loread.data.dao.FeedDao;
import me.wizos.loread.data.dao.ImgDao;
import me.wizos.loread.data.dao.RequestLogDao;
import me.wizos.loread.data.dao.TagDao;
import me.wizos.loread.net.API;

/**
 * Created by Wizos on 2016/3/12.
 */
public class WithDB {
    private static WithDB withDB;
    private TagDao tagDao;
    private FeedDao feedDao;
    private ArticleDao articleDao;
    private RequestLogDao requestLogDao;
    private ImgDao imgDao;

    private WithDB() {}


    public static WithDB getInstance() {
        if (withDB == null) { // 双重锁定，只有在 withDB 还没被初始化的时候才会进入到下一行，然后加上同步锁
            synchronized (WithDB.class) { // 同步锁，避免多线程时可能 new 出两个实例的情况
                if (withDB == null) {
                    // All init here
                    withDB = new WithDB();
                    withDB.tagDao = App.getDaoSession().getTagDao();
                    withDB.feedDao = App.getDaoSession().getFeedDao();
                    withDB.articleDao = App.getDaoSession().getArticleDao();
                    withDB.requestLogDao = App.getDaoSession().getRequestLogDao();
                    withDB.imgDao = App.getDaoSession().getImgDao();
                }
            }
        }
        return withDB;
    }

    // 自己写的
    public void saveTag(Tag tag) {
        if (tag.getId() != null) {return;}
        if (tagDao.queryBuilder().where(TagDao.Properties.Id.eq(tag.getId())).list().size() == 0) {
            tagDao.insertOrReplace(tag);
        } else { // already exist
            tagDao.update(tag);
        }
    }
    // 自己写的
    public void saveTagList(ArrayList<Tag> tags) {
        tagDao.deleteAll();
        tagDao.insertOrReplaceInTx(tags);
    }
    public List<Tag> loadTags(){
        List<Tag> tagList = tagDao.loadAll();
        return tagList;
    }

    public void saveFeed(Feed feed) {
        if (feed.getId() == null) { // new fetch
            if (feedDao.queryBuilder().where(FeedDao.Properties.Title.eq(feed.getTitle())).list().size() == 0) {
                feedDao.insertOrReplace(feed);
            }
        } else { // already exist
            feedDao.update(feed);
        }
    }


    public void saveImgs(ArrayMap<Integer, Img> imgMap) {
        for (Map.Entry<Integer, Img> entry : imgMap.entrySet()) {
            if (imgDao.queryBuilder().where(ImgDao.Properties.ArticleId.eq(entry.getValue().getArticleId()), ImgDao.Properties.No.eq(entry.getValue().getNo())).list().size() == 0) {
                imgDao.insertOrReplace(entry.getValue());
            }
        }
    }

    public void saveImg(Img img) {
        imgDao.insertOrReplace(img);
    }

    public Img getImg(String articleId, int imgNo) {
        List<Img> imgs = imgDao.queryBuilder()
                .where(ImgDao.Properties.ArticleId.eq(articleId), ImgDao.Properties.No.eq(imgNo)).list();
        if (imgs.size() != 0) {
            return imgs.get(0);
        } else {
            return null;
        }
    }

    public ArrayMap<Integer, Img> getLossImgs(String articleId) {
        QueryBuilder<Img> q = imgDao.queryBuilder()
                .where(ImgDao.Properties.ArticleId.eq(articleId), ImgDao.Properties.DownState.eq(0));
        List<Img> imgList = q.list();
        ArrayMap<Integer, Img> imgMap = new ArrayMap<>();
        for (Img img : imgList) {
            imgMap.put(img.getNo(), img);
        }
        KLog.d("==" + articleId + imgMap.size());
        return imgMap;
    }

    public SparseArray<Img> getLossImg(String articleId) {
        QueryBuilder<Img> q = imgDao.queryBuilder()
                .where(ImgDao.Properties.ArticleId.eq(articleId), ImgDao.Properties.DownState.eq(0));
        List<Img> imgList = q.list();
        SparseArray<Img> imgMap = new SparseArray<>();
        for (Img img : imgList) {
            imgMap.put(img.getNo(), img);
        }
        KLog.d("==" + articleId + imgMap.size());
        return imgMap;
    }

    public void saveArticle(Article article) {
        if (article.getId() != null) {
            articleDao.insertOrReplace(article);
        }
    }

    public void saveArticleList(List<Article> articleList) {
        if (articleList.size() != 0) { // new fetch
            articleDao.insertOrReplaceInTx(articleList);
        }
    }


    //    public Article getArticle(List articleIds) {
////        if (articleID == null) {return null;}
//        List<Article> articles = articleDao.queryBuilder().where(ArticleDao.Properties.Id.eq(articleId)).list();
//        if ( articles.size() != 0) {
//            return articles.get(0);
//        }else {
//            return null;
//        }
//    }
    public Long hasArticle(String articleName) {
        return articleDao.queryBuilder().where(ArticleDao.Properties.Title.eq(articleName)).count();
    }

    public Article getArticle(String articleId) {
//        if (articleID == null) {return null;}
        List<Article> articles = articleDao.queryBuilder().where(ArticleDao.Properties.Id.eq(articleId)).list();
        if ( articles.size() != 0) {
            return articles.get(0);
        }else {
            return null;
        }
    }

    public Article getStarredArticle(String articleId) {
//        if (articleID == null) {return null;}
        List<Article> articles = articleDao.queryBuilder().where(ArticleDao.Properties.Id.eq(articleId), ArticleDao.Properties.StarState.eq(API.ART_STAR)).list();
        if (articles.size() != 0) {
            return articles.get(0);
        } else {
            return null;
        }
    }

    public boolean hasTag(String id) {
        return tagDao.queryBuilder().where(TagDao.Properties.Id.eq(id)).list().size() > 0;
    }
    public boolean hasFeed(String url) {
        return feedDao.queryBuilder()
                .where(FeedDao.Properties.Url.eq(url)).list().size() > 0;
    }

    public void saveRequestLog( RequestLog  requestLog) {
        if(requestLog == null ){
            return;
        }
        requestLogDao.insertOrReplaceInTx( requestLog );
    }
    public void saveRequestLogList(ArrayList<RequestLog> requestLogList) {
        if (requestLogList.size() != 0) { // new fetch
            requestLogDao.insertOrReplaceInTx(requestLogList);
        }
    }

    public List<RequestLog> loadRequestListAll(){
        return  requestLogDao.loadAll();
    }
    public void delRequestListAll(){
        requestLogDao.deleteAll();
    }
    public void delRequest(RequestLog requestLog){
        requestLogDao.delete(requestLog);
    }


   /**
    * 升序
    * Collections.sort(list,Collator.getInstance(java.util.Locale.CHINA));//注意：是根据的汉字的拼音的字母排序的，而不是根据汉字一般的排序方法
    *
    * 降序
    * Collections.reverse(list);//不指定排序规则时，也是按照字母的来排序的
    **/
   public void delArtList(ArrayList<String> arrayList){
       for (String item:arrayList){
           articleDao.deleteByKey(item);
       }
   }
    public void delArtAll(List<Article> articles){
        if (articles.size() != 0) { // new fetch
            articleDao.deleteInTx( articles );
        }
    }

   public List<Article> loadArtsBeforeTime(long time){
       QueryBuilder<Article> q = articleDao.queryBuilder();
       q.where(q.and(ArticleDao.Properties.ReadState.eq(API.ART_READ), ArticleDao.Properties.StarState.eq(API.ART_UNSTAR), ArticleDao.Properties.CrawlTimeMsec.lt(time)));
//       q.list();
       return q.list();
   }

    public List<Article> loadArtsSavedBox() {
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.ReadState.eq(API.ART_READ), ArticleDao.Properties.SaveDir.eq(API.SAVE_DIR_BOX));
//        q.list();
        return q.list();
    }

    public List<Article> loadArtsSavedStore() {
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.ReadState.eq(API.ART_READ), ArticleDao.Properties.SaveDir.eq(API.SAVE_DIR_STORE));
        return q.list();
    }

    public List<Article> loadTagRead(String readState, String listTag){
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.ReadState.like(readState + "%"), ArticleDao.Properties.Categories.like("%" + listTag + "%")) /** Creates an "equal ('=')" condition  for this property. */
                .orderDesc(ArticleDao.Properties.TimestampUsec);
        return q.list();
//        long xx = System.currentTimeMillis();
//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.ReadState.like(readState + "%"), ArticleDao.Properties.Categories.like("%" + listTag + "%")) /** Creates an "equal ('=')" condition  for this property. */
//                .orderDesc(ArticleDao.Properties.TimestampUsec)
//                .build();
//        List<Article> articlelist = query.list();
//        KLog.d("【loadReadList】用时"+ (System.currentTimeMillis() - xx) + "--" + articlelist.size()  + readState + listTag  );
//        // 590-55 , 590-73,635-79, 34--612 , 82--612,83--612
//        return articlelist;
    }

    public List<Article> loadReadAll(String readState){
//        long xx = System.currentTimeMillis();
//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.ReadState.like(readState)) /** Creates an "equal ('=')" condition  for this property. */
//                .orderDesc(ArticleDao.Properties.TimestampUsec)
//                .build();


        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.ReadState.like(readState)) /** Creates an "equal ('=')" condition  for this property. */
                .orderDesc(ArticleDao.Properties.TimestampUsec);
//        List<Article> articleList = query.list();
//        KLog.d("【loadReadListAll】用时"+ (System.currentTimeMillis() - xx) + "--" + articleList.size()  + readState );
        // 1473-25
        return q.list();
    }
    public List<Article> loadArtAll(){ // 速度比要排序的全文更快
//        long xx = System.currentTimeMillis();
//        List<Article> articleList = articleDao.loadAll();
//        KLog.d("【加载所有文章无排序】用时" + (System.currentTimeMillis() - xx) +"--" +articleList.size());
        // 1473-45,1473-44,1523-67, 1527-155, 1527-49, 29--1527 , 59--1527,57--1527,65--1527,79--1527
        return articleDao.loadAll();
    }

    public List<Article> loadArtWhere() { // 速度比要排序的全文更快
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.ImgState.isNotNull()); /** Creates an "equal ('=')" condition  for this property. */
        return q.list();
    }
//    public List<Article> loadArtAllOrder(){
//        long xx = System.currentTimeMillis();
//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.ReadState.like("%")) /** Creates an "equal ('=')" condition  for this property. */
//                .orderDesc(ArticleDao.Properties.TimestampUsec)
//                .build();
//        List<Article> articleList = query.list();
//        System.out.println("【加载所有文章】" + (System.currentTimeMillis() - xx) +"--" +articleList.size());
//        return articleList;
//    }

    public List<Article> loadStarRead() {
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR), ArticleDao.Properties.SaveDir.eq(API.SAVE_DIR_CACHE)); /**  Creates an "equal ('=')" condition  for this property. */
        return q.list();
    }

    public List<Article> loadStarAll() {
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR)); /**  Creates an "equal ('=')" condition  for this property. */
        return q.list();
    }


    /**
     * 加载某个 Tag 下的 StarArt
     * @param listTag
     * @return
     */
    public List<Article> loadTagStar(String listTag){
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR),ArticleDao.Properties.Categories.like("%"+ listTag + "%")) /**  Creates an "equal ('=')" condition  for this property. */
                .orderDesc(ArticleDao.Properties.TimestampUsec);
        return q.list();

//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR),ArticleDao.Properties.Categories.like("%"+ listTag + "%")) /**  Creates an "equal ('=')" condition  for this property. */
//                .orderDesc(ArticleDao.Properties.TimestampUsec)
//                .build();
//        return query.list();
    }
    public List<Article> loadStarNoLabel(){
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR), ArticleDao.Properties.Categories.like("%" + API.U_NO_LABEL + "%")); /**  Creates an "equal ('=')" condition  for this property. */
        return q.list();
//
//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR),ArticleDao.Properties.Categories.like( "%" + API.U_NO_LABEL + "%" )) /**  Creates an "equal ('=')" condition  for this property. */
//                .build();
//        KLog.d("Star无标签：" + query.list().size());
//        return query.list();
    }

    public List<Article> loadStarListHasLabel(long userId){
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR), ArticleDao.Properties.Categories.like("%" + "user/" + userId + "/label/" + "%")); /**  Creates an "equal ('=')" condition  for this property. */
        return q.list();
//
//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.StarState.eq(API.LIST_STAR),ArticleDao.Properties.Categories.like("%" + "user/"+ userId+ "/label/" + "%")) /**  Creates an "equal ('=')" condition  for this property. */
//                .build();
//        KLog.d("Star有标签：" + query.list().size() +" " + userId );
//        return query.list();
    }
    public List<Article> loadReadListHasLabel(String readState,long userId){
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.ReadState.like(readState), ArticleDao.Properties.Categories.like("%" + "user/" + userId + "/label/" + "%"));
        return q.list();
//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.ReadState.like( readState ),ArticleDao.Properties.Categories.like("%" + "user/"+ userId+ "/label/" + "%")) /**  Creates an "equal ('=')" condition  for this property. */
//                .build();
//        KLog.d("Read有标签：" + query.list().size());
//        return query.list();
    }

    public List<Article> loadReadNoLabel(){
        QueryBuilder<Article> q = articleDao.queryBuilder()
                .where(ArticleDao.Properties.ReadState.eq(API.ART_UNREAD), ArticleDao.Properties.Categories.like("%" + API.U_NO_LABEL + "%"));
        return q.list();
//        Query query = articleDao.queryBuilder()
//                .where(ArticleDao.Properties.ReadState.eq( API.ART_UNREAD ),ArticleDao.Properties.Categories.like( "%" + API.U_NO_LABEL + "%" )) /**  Creates an "equal ('=')" condition  for this property. */
//                .build();
//        KLog.d("Read无标签：" + query.list().size());
//        return query.list();
    }


}
