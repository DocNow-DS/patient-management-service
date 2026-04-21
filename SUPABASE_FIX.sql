-- Run this in your Supabase Dashboard -> SQL Editor
-- This will create a policy to allow anyone to upload files to the medicine-reports bucket.
-- This fixes the "new row violates row-level security policy" error.

-- 1. Ensure the bucket exists and is public
INSERT INTO storage.buckets (id, name, public) 
VALUES ('medicine-reports', 'medicine-reports', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- 2. Create policy to allow anonymous uploads (INSERT)
CREATE POLICY "Allow public uploads" 
ON storage.objects FOR INSERT 
TO public 
WITH CHECK (bucket_id = 'medicine-reports');

-- 3. Create policy to allow retrieving/viewing files (SELECT)
CREATE POLICY "Allow public read" 
ON storage.objects FOR SELECT 
TO public 
USING (bucket_id = 'medicine-reports');

-- 4. Create policy to allow updating files (UPDATE)
CREATE POLICY "Allow public update" 
ON storage.objects FOR UPDATE 
TO public 
USING (bucket_id = 'medicine-reports');

-- 5. Create policy to allow deleting files (DELETE)
CREATE POLICY "Allow public delete" 
ON storage.objects FOR DELETE 
TO public 
USING (bucket_id = 'medicine-reports');
