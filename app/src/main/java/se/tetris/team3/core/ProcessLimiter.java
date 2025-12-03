package se.tetris.team3.core;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

/**
 * Windows Job Object를 사용하여 프로세스 메모리를 제한하는 유틸리티
 * Windows 전용
 */
public class ProcessLimiter {

    // Kernel32 인터페이스 정의
    public interface Kernel32Extended extends StdCallLibrary {
        Kernel32Extended INSTANCE = Native.load("kernel32", Kernel32Extended.class, W32APIOptions.DEFAULT_OPTIONS);

        HANDLE CreateJobObjectA(Pointer lpJobAttributes, String lpName);
        boolean SetInformationJobObject(HANDLE hJob, int jobObjectInfoClass, Pointer lpJobObjectInfo, int cbJobObjectInfoLength);
        boolean AssignProcessToJobObject(HANDLE hJob, HANDLE hProcess);
        HANDLE GetCurrentProcess();
        boolean CloseHandle(HANDLE hObject);
    }

    // Job Object 정보 클래스
    public static final int JobObjectExtendedLimitInformation = 9;

    // Limit flags
    public static final int JOB_OBJECT_LIMIT_PROCESS_MEMORY = 0x00000100;
    public static final int JOB_OBJECT_LIMIT_JOB_MEMORY = 0x00000200;

    // JOBOBJECT_BASIC_LIMIT_INFORMATION 구조체
    public static class JOBOBJECT_BASIC_LIMIT_INFORMATION extends Structure {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public int LimitFlags;
        public long MinimumWorkingSetSize;
        public long MaximumWorkingSetSize;
        public int ActiveProcessLimit;
        public long Affinity;
        public int PriorityClass;
        public int SchedulingClass;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("PerProcessUserTimeLimit", "PerJobUserTimeLimit", "LimitFlags",
                    "MinimumWorkingSetSize", "MaximumWorkingSetSize", "ActiveProcessLimit",
                    "Affinity", "PriorityClass", "SchedulingClass");
        }
    }

    // IO_COUNTERS 구조체
    public static class IO_COUNTERS extends Structure {
        public long ReadOperationCount;
        public long WriteOperationCount;
        public long OtherOperationCount;
        public long ReadTransferCount;
        public long WriteTransferCount;
        public long OtherTransferCount;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("ReadOperationCount", "WriteOperationCount", "OtherOperationCount",
                    "ReadTransferCount", "WriteTransferCount", "OtherTransferCount");
        }
    }

    // JOBOBJECT_EXTENDED_LIMIT_INFORMATION 구조체
    public static class JOBOBJECT_EXTENDED_LIMIT_INFORMATION extends Structure {
        public JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
        public IO_COUNTERS IoInfo;
        public long ProcessMemoryLimit;
        public long JobMemoryLimit;
        public long PeakProcessMemoryUsed;
        public long PeakJobMemoryUsed;

        public JOBOBJECT_EXTENDED_LIMIT_INFORMATION() {
            BasicLimitInformation = new JOBOBJECT_BASIC_LIMIT_INFORMATION();
            IoInfo = new IO_COUNTERS();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("BasicLimitInformation", "IoInfo", "ProcessMemoryLimit",
                    "JobMemoryLimit", "PeakProcessMemoryUsed", "PeakJobMemoryUsed");
        }
    }

    /**
     * 현재 프로세스에 메모리 제한을 설정 (Windows 전용)
     * 
     * @param limitMB 메모리 제한 (MB 단위)
     * @return 성공 여부
     */
    public static boolean setMemoryLimit(long limitMB) {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("windows")) {
            System.err.println("ProcessLimiter: Only supported on Windows");
            return false;
        }

        try {
            Kernel32Extended kernel32 = Kernel32Extended.INSTANCE;

            // Job Object 생성
            HANDLE hJob = kernel32.CreateJobObjectA(null, null);
            if (hJob == null || hJob.getPointer() == Pointer.NULL) {
                System.err.println("Failed to create Job Object");
                return false;
            }

            // 제한 정보 설정
            JOBOBJECT_EXTENDED_LIMIT_INFORMATION limitInfo = new JOBOBJECT_EXTENDED_LIMIT_INFORMATION();
            limitInfo.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_PROCESS_MEMORY;
            limitInfo.ProcessMemoryLimit = limitMB * 1024 * 1024; // MB to Bytes

            // 구조체를 네이티브 메모리에 쓰기
            limitInfo.write();

            // Job Object에 제한 정보 설정
            boolean setInfoResult = kernel32.SetInformationJobObject(
                    hJob,
                    JobObjectExtendedLimitInformation,
                    limitInfo.getPointer(),
                    limitInfo.size()
            );

            if (!setInfoResult) {
                System.err.println("Failed to set Job Object information");
                kernel32.CloseHandle(hJob);
                return false;
            }

            // 현재 프로세스를 Job Object에 할당
            HANDLE hProcess = kernel32.GetCurrentProcess();
            boolean assignResult = kernel32.AssignProcessToJobObject(hJob, hProcess);

            if (!assignResult) {
                System.err.println("Failed to assign process to Job Object");
                kernel32.CloseHandle(hJob);
                return false;
            }

            System.out.println("Memory limit set to " + limitMB + "MB via Job Object");
            return true;

        } catch (Exception e) {
            System.err.println("Error setting memory limit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
