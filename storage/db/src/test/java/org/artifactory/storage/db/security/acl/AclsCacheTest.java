package org.artifactory.storage.db.security.acl;

import org.testng.annotations.Test;

/**
 * @author Gidi Shabat
 */
@Test
public class AclsCacheTest {
    //
    ///**
    // * Make sure that all thread are getting released by timeout
    // */
    //public void test1(){
    //    AclServiceImpl.AclsCache aclsCache = new AclServiceImpl.AclsCache(1000,new AclCacheLoaderTest());
    //    ArrayList<Map<String, AclInfo>> list = new ArrayList<>();
    //    list.add(aclsCache.get());
    //    aclsCache.promoteAclsDbVersion();
    //    for (int i = 0; i < 10; i++) {
    //         new Thread(() -> {
    //             list.add(aclsCache.get());
    //         }).start();
    //
    //    }
    //    while(list.size()<11){
    //        waitSecond();
    //    }
    //    for (int i = 0; i < 10; i++) {
    //        Map<String, AclInfo> map = list.get(i);
    //        Assert.assertEquals(map.keySet().iterator().next(),"0");
    //    }
    //    Assert.assertEquals(list.get(10).keySet().iterator().next(),"1");
    //}
    //
    ///**
    // * Make sure that all thread are getting blocked while the map is null
    // */
    //public void test2(){
    //    AclServiceImpl.AclsCache aclsCache = new AclServiceImpl.AclsCache(1000,new AclCacheLoaderTest());
    //    ArrayList<Map<String, AclInfo>> list = new ArrayList<>();
    //    for (int i = 0; i < 10; i++) {
    //        new Thread(() -> {
    //            list.add(aclsCache.get());
    //        }).start();
    //
    //    }
    //    while(list.size()<10){
    //        waitSecond();
    //    }
    //    for (int i = 0; i < 10; i++) {
    //        Map<String, AclInfo> map = list.get(i);
    //        Assert.assertEquals(map.keySet().iterator().next(),"0");
    //    }
    //}
    //
    ///**
    // * Make sure that all thread are getting blocked while the map is null
    // */
    //public void test3(){
    //    AclServiceImpl.AclsCache aclsCache = new AclServiceImpl.AclsCache(30000,new AclCacheLoaderTest());
    //    ArrayList<Map<String, AclInfo>> list = new ArrayList<>();
    //    for (int i = 0; i < 10; i++) {
    //        new Thread(() -> {
    //            list.add(aclsCache.get());
    //        }).start();
    //
    //    }
    //    while(list.size()<10){
    //        waitSecond();
    //    }
    //    for (int i = 0; i < 10; i++) {
    //        Map<String, AclInfo> map = list.get(i);
    //        Assert.assertEquals(map.keySet().iterator().next(),"0");
    //    }
    //}
    //
    //private void waitSecond() {
    //    try {
    //        Thread.sleep(1000);
    //    } catch (InterruptedException e) {
    //        // Do nothing
    //    }
    //}
    //
    //
    //public static class AclCacheLoaderTest extends AclServiceImpl.AclCacheLoader{
    //
    //    AtomicInteger version=new AtomicInteger();
    //
    //    public AclCacheLoaderTest() {
    //        super(null,null,null);
    //    }
    //
    //    @Override
    //    public Map<String, AclInfo> call() {
    //        HashMap<String, AclInfo> map = new HashMap<>();
    //        try {
    //            Thread.sleep(10000);
    //        } catch (InterruptedException e) {
    //           // Do nothing
    //        }
    //        map.put(""+version.getAndAdd(1),null);
    //        return map;
    //    }
    //}
}
