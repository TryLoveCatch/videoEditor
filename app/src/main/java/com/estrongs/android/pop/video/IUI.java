package com.estrongs.android.pop.video;

interface IUI {
    
    /**
     * 初始化view属性，在此之前已通过ButterKnife初始化view
     * @Title: initViewProperty
     * @return void
     * @date Apr 18, 2014 11:00:56 AM
     */
    void initViewProperty();

    /**
     * 初始化数据请求相关
     * @Title: initData
     * @return void
     * @date Apr 24, 2014 10:38:59 AM
     */
    void initData();
    
}
