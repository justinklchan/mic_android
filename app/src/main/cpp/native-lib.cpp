#include <jni.h>
#include <string.h>
#include <fftw3.h>
#include <math.h>

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_example_microphone_OfflineRecorder_fftnative_1short(JNIEnv *env, jclass thiz, jshortArray data,
                                                        jint N) {
    fftw_complex *in , *out;
    fftw_plan p;

    jshort *shortArray = env->GetShortArrayElements(data, NULL);
    int datalen = env -> GetArrayLength(data);

    in = (fftw_complex*) fftw_malloc(sizeof(fftw_complex) * datalen);
    out = (fftw_complex*) fftw_malloc(sizeof(fftw_complex) * datalen);

    for (int i = 0; i < datalen; i++) {
        in[i][0] = 0;
        in[i][1] = 0;
        out[i][0] = 0;
        out[i][1] = 0;
    }

    for (int i = 0; i < datalen; i++) {
        in[i][0] = shortArray[i];
    }

    p = fftw_plan_dft_1d(N, in, out, FFTW_FORWARD, FFTW_ESTIMATE);
    fftw_execute(p);

    jdouble mag[N];
    for (int i = 0; i < N; i++) {
        double real = out[i][0];
        double imag = out[i][1];

        mag[i] = sqrt((real*real)+(imag*imag));
        mag[i] = 20*log10(mag[i]);
    }

    jdoubleArray result;
    result = env->NewDoubleArray(N);
    env->SetDoubleArrayRegion(result, 0, N, mag);

    fftw_destroy_plan(p);
    fftw_free(in); fftw_free(out);

    return result;
}
