/*
 * JCuda - Java bindings for NVIDIA CUDA driver and runtime API
 * http://www.jcuda.org
 *
 * Copyright 2011 Marco Hutter - http://www.jcuda.org
 */
import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

import java.io.*;
import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;

import jcuda.*;
import jcuda.driver.*;
import static jcuda.driver.JCudaDriver.*;

/**
 * This is a sample class demonstrating how to use the JCuda driver
 * bindings to load and execute a CUDA vector addition kernel.
 * The sample reads a CUDA file, compiles it to a PTX file
 * using NVCC, loads the PTX file as a module and executes
 * the kernel function. <br />
 */
public class TestView
{
    /**
     * Entry point of this sample
     *
     * @param args Not used
     * @throws IOException If an IO error occurs
     */
    public static void main(String args[]) throws IOException
    {

        ImageWriter writer = new TIFFImageWriterSpi().createWriterInstance();
        writer.setOutput(new FileImageOutputStream(new File("test.tif")));
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        writer.prepareWriteEmpty(null, ImageTypeSpecifier.createBanded(ColorSpace.getInstance(ColorSpace.CS_GRAY), 
                new int[]{0}, new int[]{0}, DataBuffer.TYPE_BYTE, false, false), 1000, 1000, null, null, null);
        
        for(int i = 0; i < 10; i++) {
            WritableRaster r = raster.createCompatibleWritableRaster();
            for(int j = 0; j < 10000; j++)
                r.getDataBuffer().setElem(j, i);
            writer.prepareReplacePixels(0, new Rectangle(i*100, i*100, 100, 100));
            TIFFImageWriteParam param = new TIFFImageWriteParam(Locale.FRENCH);
            //param.setSourceRegion(new Rectangle(i*100, i*100, 100, 100));
            param.setDestinationOffset(new Point(i*100, i*100));
            param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
            param.setTiling(100, 100, 0, 0);
            writer.replacePixels(r, param);
            writer.endReplacePixels();
        }
        
        writer.endWriteEmpty();
        writer.dispose();
        
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = preparePtxFile("/home/gvuidel/Documents/PImage/pimage/target/classes/view_kernel.cu");

        // Initialize the driver and create a context for the first device.
        cuInit(0);
        
        int [] nbDev = new int[1];
        cuDeviceGetCount(nbDev);
        
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        // Load the ptx file.
        CUmodule module = new CUmodule();
        cuModuleLoad(module, ptxFileName);

        // Obtain a functions pointer
        CUfunction funViewShed = new CUfunction();
        cuModuleGetFunction(funViewShed, module, "viewShed");
        CUfunction funSumV = new CUfunction();
        cuModuleGetFunction(funSumV, module, "sumV");
        CUfunction funClearView = new CUfunction();
        cuModuleGetFunction(funClearView, module, "clearView");
        CUfunction funSumView = new CUfunction();
        cuModuleGetFunction(funSumView, module, "sumView");
        
        final int NCORE = 512;
        final int N = 1800;
        int sumSize = N/2*(N/NCORE+1);
        // Allocate and fill the host input data
        float dtm[] = new float[N*N];
        int [] sumView = new int[N*N];
        for(int i = 0; i < N*N; i++) {
            dtm[i] = (float)Math.random();
        }

        // Allocate the device input data, and copy the
        // host input data to the device
        CUdeviceptr dtmDev = new CUdeviceptr();
        cuMemAlloc(dtmDev, N*N * Sizeof.FLOAT);
        cuMemcpyHtoD(dtmDev, Pointer.to(dtm), N*N * Sizeof.FLOAT);

        CUdeviceptr sumDev = new CUdeviceptr();
        cuMemAlloc(sumDev, sumSize * Sizeof.INT);
        
        // Allocate device output memory
        CUdeviceptr viewDev = new CUdeviceptr();
        cuMemAlloc(viewDev, N*N * Sizeof.CHAR);
        CUdeviceptr sumViewDev = new CUdeviceptr();
        cuMemAlloc(sumViewDev, N*N * Sizeof.INT);
        cuMemcpyHtoD(sumViewDev, Pointer.to(sumView), N*N * Sizeof.INT);

        for(int j = 0; j < 10; j++) {
//            Pointer clearSumParam = Pointer.to(
//                Pointer.to(sumDev)
//            );
//            cuLaunchKernel(funClearSum,
//                1,  1, 1,      // Grid dimension
//                NCORE, 1, 1,      // Block dimension
//                0, null,               // Shared memory size and stream
//                clearSumParam, null // Kernel- and extra parameters
//            );
//            cuCtxSynchronize();

            Pointer clearViewParam = Pointer.to(
                Pointer.to(viewDev),
                Pointer.to(new int[]{N*N/4})
            );
            cuLaunchKernel(funClearView,
                N, N/(4*NCORE)+1, 1,     // Grid dimension
                NCORE, 1, 1,      // Block dimension
                0, null,               // Shared memory size and stream
                clearViewParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();
            
            Pointer viewShedParam = Pointer.to(
                Pointer.to(new int[]{N/2}),
                Pointer.to(new int[]{N/2}),
                Pointer.to(new float[]{1.5f}),
                Pointer.to(dtmDev),
                Pointer.to(new int[]{N}),
                Pointer.to(new int[]{N}),
                Pointer.to(new float[]{1}),
                Pointer.to(new float[]{10}),
                Pointer.to(viewDev)
            );
            cuLaunchKernel(funViewShed,
                4*(N/NCORE+1),  1, 1,      // Grid dimension
                NCORE, 1, 1,      // Block dimension
                0, null,               // Shared memory size and stream
                viewShedParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();

            Pointer sumVParam = Pointer.to(
                Pointer.to(viewDev),
                Pointer.to(new int[]{N*N}),
                Pointer.to(sumDev)
            );
            cuLaunchKernel(funSumV,
                sumSize/100+1,  100, 1,      // Grid dimension
                NCORE, 1, 1,      // Block dimension
                2*NCORE*Sizeof.INT, null,               // Shared memory size and stream
                sumVParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();
            
            Pointer sumViewParam = Pointer.to(
                Pointer.to(viewDev),
                Pointer.to(sumViewDev),
                Pointer.to(new int[]{N*N})
            );
            cuLaunchKernel(funSumView,
                N, N/NCORE+1, 1,     // Grid dimension
                NCORE, 1, 1,      // Block dimension
                0, null,               // Shared memory size and stream
                sumViewParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();



            // Allocate host output memory and copy the device output
            // to the host.
            int sum[] = new int[sumSize];
            cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
            // Verify the result
            int nb = 0;
            for(int i = 0; i < sumSize; i++) {
                nb += sum[i];
            }
            System.out.println("Nb : " + nb);
        }
        
        cuMemcpyDtoH(Pointer.to(sumView), sumViewDev, N*N * Sizeof.INT);
        
        byte [] view = new byte[N*N];
        cuMemcpyDtoH(Pointer.to(view), viewDev, N*N * Sizeof.CHAR);
        // Verify the result
        int nb = 0;
        for(int i = 0; i < N*N; i++) {
            nb += view[i];
        }
        System.out.println("Nb : " + nb);

        // Clean up.
        cuMemFree(dtmDev);
        cuMemFree(viewDev);
        cuMemFree(sumDev);
    }

    /**
     * The extension of the given file name is replaced with "ptx".
     * If the file with the resulting name does not exist, it is
     * compiled from the given file using NVCC. The name of the
     * PTX file is returned.
     *
     * @param cuFileName The name of the .CU file
     * @return The name of the PTX file
     * @throws IOException If an I/O error occurs
     */
    private static String preparePtxFile(String cuFileName) throws IOException
    {
        int endIndex = cuFileName.lastIndexOf('.');
        if (endIndex == -1)
        {
            endIndex = cuFileName.length()-1;
        }
        String ptxFileName = cuFileName.substring(0, endIndex+1)+"ptx";
        File ptxFile = new File(ptxFileName);
        if (ptxFile.exists())
        {
            return ptxFileName;
        }

        File cuFile = new File(cuFileName);
        if (!cuFile.exists())
        {
            throw new IOException("Input file not found: "+cuFileName);
        }
        String modelString = "-m"+System.getProperty("sun.arch.data.model");
        String command =
            "nvcc " + modelString + " -ptx "+
            cuFile.getPath()+" -o "+ptxFileName;

        System.out.println("Executing\n"+command);
        Process process = Runtime.getRuntime().exec(command);

        String errorMessage =
            new String(toByteArray(process.getErrorStream()));
        String outputMessage =
            new String(toByteArray(process.getInputStream()));
        int exitValue = 0;
        try
        {
            exitValue = process.waitFor();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(
                "Interrupted while waiting for nvcc output", e);
        }

        if (exitValue != 0)
        {
            System.out.println("nvcc process exitValue "+exitValue);
            System.out.println("errorMessage:\n"+errorMessage);
            System.out.println("outputMessage:\n"+outputMessage);
            throw new IOException(
                "Could not create .ptx file: "+errorMessage);
        }

        System.out.println("Finished creating PTX file");
        return ptxFileName;
    }

    /**
     * Fully reads the given InputStream and returns it as a byte array
     *
     * @param inputStream The input stream to read
     * @return The byte array containing the data from the input stream
     * @throws IOException If an I/O error occurs
     */
    private static byte[] toByteArray(InputStream inputStream)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[8192];
        while (true)
        {
            int read = inputStream.read(buffer);
            if (read == -1)
            {
                break;
            }
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }


}
