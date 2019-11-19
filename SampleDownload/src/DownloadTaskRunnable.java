
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Response;
import sun.rmi.runtime.Log;

public class DownloadTaskRunnable implements Runnable {

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;
    private static final String TAG = "下载信息";

    private int blockid;
    private DownloadInfo mDownloadInfo;
    private OkhttpManager mOkhttpManager;
    private DOWNLOAD_TASK_FUN mTASK_fun;


    private RandomAccessFile rf;
    private File file;
    private Response response;
    private InputStream in = null;

    public DownloadTaskRunnable(DownloadInfo info, int blockid,DOWNLOAD_TASK_FUN Interface) {
        this.mDownloadInfo = info;
        this.blockid = blockid;
        mOkhttpManager = OkhttpManager.getInstance();
        mTASK_fun = Interface;

    }

    /**
     * @throws FileNotFoundException 做一些初始化工作，创建file，randomAccessfile，获取response
     */
    private void initFile() {
        try {
            response=OkhttpManager.getInstance().getResponse(mDownloadInfo,blockid);
            System.out.println("fileName=" + mDownloadInfo.getFileName() + " 每个线程负责下载文件大小contentLength=" + response.body().contentLength()
                    + " 开始位置start=" + mDownloadInfo.splitStart[blockid] + "结束位置end=" + mDownloadInfo.splitEnd[blockid] + " threadId=" + blockid);
            file = new File(mDownloadInfo.getPath() + mDownloadInfo.getFileName());
            rf = new RandomAccessFile(file, "rw");
            rf.seek(mDownloadInfo.splitStart[blockid]);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void closeThings() {
        try {
            if (response != null) {
                response.close();
            }
            if (rf != null) {
                rf.close();
            }
            if (in!=null){
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            initFile();

            if (response != null) {
                this.in = response.body().byteStream();

                byte b[] = new byte[1024];

                //是记录现在开始下载的长度
                //long lengthNow = mDownloadInfo.splitStart[blockid];
                //从流中读取的数据长度
                int len;
                while ((len = in.read(b)) != -1) {

                    if (mDownloadInfo.isPause()) {
                        mTASK_fun.pausedDownload();
                    } else if (mDownloadInfo.isCancel()) {
                        mTASK_fun.canceledDownload();
                    }else{
                        rf.write(b, 0, len);
                        //计算出总的下载长度
                        mDownloadInfo.splitStart[blockid] += len;
                        mTASK_fun.updateProcess(mDownloadInfo);
                    }

                }
                mTASK_fun.downloadSucess();

            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }finally {
            closeThings();
        }

    }
}