package org.jeecg.modules.qywx.api.message.vo;

import com.jeecg.qywx.api.message.vo.NewsArticle;

public class NewsEntity {
	private NewsArticle[] articles;// 定义数组图文消息，一个图文消息支持1到8条图文

	public NewsArticle[] getArticles() {
		return articles;
	}

	public void setArticles(NewsArticle[] articles) {
		this.articles = articles;
	}

	

	

}
