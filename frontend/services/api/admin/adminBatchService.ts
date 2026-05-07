import axios from "@/lib/axiosInstance";

export interface BatchTriggerResponse {
  jobInstanceId: number;
  executionId: number;
  status: string;
}

export interface BatchJobStatusResponse {
  status: string;
  startTime: string | null;
  endTime: string | null;
  readCount: number;
  writeCount: number;
  skipCount: number;
}

export async function adminTriggerBatch(): Promise<BatchTriggerResponse> {
  const res = await axios.post<BatchTriggerResponse>("/api/v1/admin/batch/trigger");
  return res.data;
}

export async function adminBatchStatus(executionId: number): Promise<BatchJobStatusResponse> {
  const res = await axios.get<BatchJobStatusResponse>(`/api/v1/admin/batch/status/${executionId}`);
  return res.data;
}

