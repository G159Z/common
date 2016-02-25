package me.suanmiao.common.io.http.image.spice;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;

import me.suanmiao.common.io.cache.mmbean.AbstractMMBean;
import me.suanmiao.common.io.http.image.Photo;
import me.suanmiao.common.util.LogUtil;

/**
 * Created by suanmiao on 14/12/7.
 */
public class PhotoSpiceRequest extends BaseCacheImageRequest<Photo> {

  protected static final String KEY_CONTENT_LENGTH = "Content-Length";
  protected Photo photo;
  protected boolean shouldCache = true;
  protected Photo.LoadSource loadSource = Photo.LoadSource.BOTH;

  public PhotoSpiceRequest(Photo photo) {
    super(Photo.class);
    this.photo = photo;
  }

  public PhotoSpiceRequest(Photo photo, Photo.LoadSource option) {
    super(Photo.class);
    this.photo = photo;
    this.loadSource = option;
  }

  @Override
  public Photo loadDataFromNetwork() throws IOException {
    return loadNormalPhoto();
  }

  private Photo loadNormalPhoto() throws IOException {
    switch (loadSource) {
      case ONLY_FROM_CACHE: {
        notifyProgress(photo, 0);
        AbstractMMBean cacheContent = getCacheManager().get(photo.getCacheKey());
        if (cacheContent != null) {
          notifyProgress(photo, 1f);
        }
        photo.setContent(cacheContent);
      }
        break;
      case ONLY_FROM_NETWORK: {
        AbstractMMBean networkContent = getMMFromNetwork();
        if (shouldCache && networkContent != null) {
          getCacheManager().put(photo.getCacheKey(), networkContent, true);
        }
        notifyProgress(photo, 1f);
        photo.setContent(networkContent);
      }
        break;
      case BOTH: {
        AbstractMMBean content = getCacheManager().get(photo.getCacheKey());
        if (content == null) {
          content = getMMFromNetwork();
          if (shouldCache && content != null) {
            getCacheManager().put(photo.getCacheKey(), content, true);
          }
        }
        notifyProgress(photo, 1f);
        photo.setContent(content);
      }
        break;
    }
    return photo;
  }

  protected AbstractMMBean getMMFromNetwork() throws IOException {
    Request request = new Request.Builder()
        .url(photo.getUrl())
        .build();
    Response response = getOkHttpClient().newCall(request).execute();
    /**
     * get content length
     */
    String contentLength = response.header(KEY_CONTENT_LENGTH, "0");
    photo.setContentLength(Integer.parseInt(contentLength));

    InputStream in = response.body().byteStream();
    if (response.code() == 200){
      return getCacheManager().getBeanGenerator().constructMMBeanFromNetworkStream(
        photo, in);
    }else{
      LogUtil.logE("error when getMMFromNetwork for url:",photo.getUrl(),"status code:",response.code());
      return null;
    }
  }

  private void notifyProgress(Photo photo, float progress) {
    if (photo.getLoadOption().progressListener != null) {
      photo.getLoadOption().progressListener.onProgress(progress);
    }
  }

  public boolean isShouldCache() {
    return shouldCache;
  }

  public void setShouldCache(boolean shouldCache) {
    this.shouldCache = shouldCache;
  }

}
