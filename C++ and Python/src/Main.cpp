#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <fstream>
#include <direct.h>

using namespace cv;
using namespace std;

int H_MIN = 0;
int H_MAX = 169;
int S_MIN = 0;
int S_MAX = 255;
int V_MIN = 0;
int V_MAX = 255;

bool compareContourAreas ( std::vector<cv::Point> contour1, std::vector<cv::Point> contour2 ) {
	double i = fabs( contourArea(cv::Mat(contour1)) );
	double j = fabs( contourArea(cv::Mat(contour2)) );
	return ( i < j );
}

int main( int argc, char** argv )
{

	ofstream jsonfile("data.json");

	Mat input = imread("in.jpeg", CV_LOAD_IMAGE_COLOR);
	Mat input_gray;
	resize(input, input, Size(0,0), 0.25, 0.25);
	cvtColor(input, input_gray, CV_BGR2GRAY);
	Mat frame_hsv;
	cvtColor(input, frame_hsv, CV_BGR2HSV);


	/*namedWindow("Trackbar");

	createTrackbar("H-MIN", "Trackbar", &H_MIN, 180);
	createTrackbar("H-MAX", "Trackbar", &H_MAX, 180);
	createTrackbar("S-MIN", "Trackbar", &S_MIN, 255);
	createTrackbar("S-MAX", "Trackbar", &S_MAX, 255);
	createTrackbar("V-MIN", "Trackbar", &V_MIN, 255);
	createTrackbar("V-MAX", "Trackbar", &V_MAX, 255);*/

	int KK=1;
	while(KK)
	{
		KK=0;
		Mat thresh_frame(input.size(), CV_8UC3);

		for(int x=0;x<input.cols;x++)
		{
			for(int y=0;y<input.rows;y++)
			{
				Vec3b intensity1 = frame_hsv.at<Vec3b>(y, x);
				uchar H = intensity1.val[0];
				uchar S = intensity1.val[1];
				uchar V = intensity1.val[2];
				if(H_MIN<=H && H<=H_MAX
					&& S_MIN<=S && S<=S_MAX && V_MIN<=V && V<=V_MAX)
				{

					thresh_frame.at<Vec3b>(y,x) = Vec3b(0,0,0);
				}
				else{
					thresh_frame.at<Vec3b>(y,x) = Vec3b(255,255,255);
				}
			}
		}
		imwrite("out_1.png", thresh_frame);
		medianBlur(thresh_frame, thresh_frame, 5);
		imwrite("out_2.png", thresh_frame);
		std::vector<std::vector<cv::Point> > contours;
		std::vector<cv::Vec4i> hierarchy;

		Mat gray;
		cvtColor(thresh_frame, gray, CV_BGR2GRAY);
		findContours(gray.clone(), contours, hierarchy, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

		std::sort(contours.begin(), contours.end(), compareContourAreas);

		vector<Point2d> data;

		int contours_size = (int)contours.size();

		for(int i = contours_size -1; i >= contours_size-8;i--)
		{
			Rect r = boundingRect(contours[i]);
			rectangle(thresh_frame, r, Scalar(0,0,255));
			string label = to_string(i);
			putText(thresh_frame, label, Point(r.x, r.y), CV_FONT_HERSHEY_PLAIN, 0.8, Scalar(0,255,0));
		}

		imshow("THRESH", thresh_frame);
		imwrite("out_3.png", thresh_frame);

		waitKey(30);

		string json = "{\"trainX\":[";
		string trainX = "", trainY = "", qualityX = "", qualityY = "", sample = "";

		for(int i = contours_size -1; i >= contours_size-8;i--)
		{

			Rect r = boundingRect(contours[i]);
			string label = to_string(i);


			double intensity_sum = 0;
			int n = 0;
			for(int x = r.x; x<=r.x + r.width; x++)
			{
				for(int y = r.y; y<=r.y + r.height; y++)
				{
					uchar B = gray.at<uchar>(y,x);

					if(B > 0)
					{
						gray.at<uchar>(y,x) = 144;
						int I = (int)(input_gray.at<uchar>(y,x));
						intensity_sum += I;
						n += 1;
					}
				}
			}


			

			double intensity = intensity_sum / n;

			cout << label << ": " << intensity << endl;

			string s;
			cout << "Conc.: ";
			cin >> s;

			double conc = 0;
			if(s[s.length()-1] == 'Q')
			{
				conc = stod(s.substr(0,s.length()-1));
				qualityX += to_string(intensity) + ",";
				qualityY += to_string(conc) + ",";

			}
			else if(s[s.length()-1] == 'S')
				sample = to_string(intensity);
			else
			{
				conc = stod(s);
				trainX += to_string(intensity) + ",";
				trainY += to_string(conc) + ",";
			}


		}

		trainX[trainX.length()-1]=']';
		trainY[trainY.length()-1]=']';
		qualityY[qualityY.length()-1]=']';
		qualityX[qualityX.length()-1]=']';

		json += trainX + ", \"trainY\":[";
		json += trainY + ", \"qualityX\":[";
		json += qualityX + ", \"qualityY\":[";
		json += qualityY + ",\"sample\":"+ sample +"}";


		jsonfile << json;

		jsonfile.close();


	}

	return 0;
}