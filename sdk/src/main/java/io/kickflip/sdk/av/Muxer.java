package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.google.common.eventbus.EventBus;

import java.nio.ByteBuffer;

import io.kickflip.sdk.events.MuxerFinishedEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base Muxer class for interaction with MediaCodec based
 * encoders
 */
public abstract class Muxer {
    private static final String TAG = "Muxer";

    public static enum FORMAT { MPEG4, HLS, RTMP }

    private final int mExpectedNumTracks = 2;           // TODO: Make this configurable?

    protected FORMAT mFormat;
    protected String mOutputPath;
    protected int mNumTracks;
    protected int mNumTracksFinished;

    private EventBus mEventBus;

    protected Muxer(String outputPath, FORMAT format){
        Log.i(TAG, "Created muxer for output: " + outputPath);
        mOutputPath = checkNotNull(outputPath);
        mFormat = format;
        mNumTracks = 0;
        mNumTracksFinished = 0;
    }

    public void setEventBus(EventBus eventBus){
        mEventBus = eventBus;
    }

    public String getOutputPath(){
        return mOutputPath;
    }

    /**
     * Adds the specified track and returns the track index
     *
     * @param trackFormat MediaFormat of the track to add. Gotten from MediaCodec#dequeueOutputBuffer
     *                    when returned status is INFO_OUTPUT_FORMAT_CHANGED
     * @return index of track in output file
     */
    public int addTrack(MediaFormat trackFormat){
        mNumTracks++;
        return mNumTracks - 1;
    }

    /**
     * Called by the hosting Encoder
     * to notify the Muxer that it should no
     * longer assume the Encoder resources are available.
     *
     */
    public void onEncoderReleased(int trackIndex){
    }

    public void release(){
        if(mEventBus != null)
            mEventBus.post(new MuxerFinishedEvent());
    }

    public boolean isStarted(){
        return false;
    }

    /**
     * Write the MediaCodec output buffer. This method <b>must</b>
     * be overridden by subclasses to release encodedData, transferring
     * ownership back to encoder, by calling encoder.releaseOutputBuffer(bufferIndex, false);
     *
     * @param trackIndex
     * @param encodedData
     * @param bufferInfo
     */
    public void writeSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo){
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            signalEndOfTrack();
        }
    }


    protected boolean allTracksFinished(){
        return (mNumTracks == mNumTracksFinished);
    }

    protected boolean allTracksAdded(){
        return (mNumTracks == mExpectedNumTracks);
    }

    /**
     * Muxer will call this itself if it detects BUFFER_FLAG_END_OF_STREAM
     * in writeSampleData.
     */
    public void signalEndOfTrack(){
        mNumTracksFinished++;
    }

    /**
     * Does this Muxer's format require AAC ADTS headers?
     * see http://wiki.multimedia.cx/index.php?title=ADTS
     * @return
     */
    protected boolean formatRequiresADTS(){
        switch(mFormat){
            case HLS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Does this Muxer's format require
     * copying and buffering encoder output buffers.
     * Generally speaking, is the output a Socket or File?
     * @return
     */
    protected boolean formatRequiresBuffering(){
        switch(mFormat){
            case RTMP:
               return true;
            default:
                return false;
        }
    }
}