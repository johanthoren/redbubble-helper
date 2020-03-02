(ns rbh.core-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [rbh.core :refer :all]))

(def testdir "test/rbh-test-tmp/")
(def test-files-to-copy
  ["resources/test_1.svg"
   "resources/test_2.png"])
(def testsvg (str testdir "test_1.svg"))
(def testpng (str testdir "test_2.png"))
(def output-png-1 (str testdir "test_1_output.png"))

(defn set-up-test-dir
  [f]
  (fs/delete-dir testdir) ; Remove any old testdir
  (fs/mkdirs testdir) ; Create a new tmp dir
  (doseq [file test-files-to-copy]
    (fs/copy file (str testdir (fs/base-name file))))
  (f)
  (fs/delete-dir testdir))

(use-fixtures :each set-up-test-dir)

(deftest test-svg?
  (testing "Testing that svg images are correctly identified."
    (is (svg? testsvg))
    (is (not (svg? testpng)))))

(deftest test-png?
  (testing "Testing that png images are correctly identified."
    (is (png? testpng))
    (is (not (png? testsvg)))))

(deftest test-generate-png!
  (testing "Testing that a png file is generated with the correct dimensions."
    (let [t (generate-png! testsvg output-png-1)]
      (is (thrown? java.lang.AssertionError
                   (generate-png! testpng output-png-1 :width 13500 :height 9505)))
      (is (thrown? java.lang.NumberFormatException
                   (generate-png! testsvg output-png-1 :width 13500 :height "foo")))
      (is (thrown? java.lang.NumberFormatException
                   (generate-png! testsvg output-png-1 :width "foo" :height 9505)))
      (is (= 1350 (:width (generate-png! testsvg output-png-1 :width 1350))))
      (is (= 950 (:height (generate-png! testsvg output-png-1 :height 950))))
      (is (= "ffffff00" (:background t)))
      (is (png? (:file t)))
      (is (not (svg? (:file t))))
      (is (= clojure.lang.PersistentArrayMap (type t))))))

(deftest test-png-dimensions?
  (testing "Testing that the dimensions are properly read from a png file."
    (is (thrown? java.lang.AssertionError (png-dimensions? testsvg)))
    (is (= {:width 13500 :height 9505} (png-dimensions? testpng)))))

(deftest test-greater-dimensions?
  (testing "Testing that the correct dimension is interpreted as greater."
    (let [dim1 {:width 2 :height 1}
          dim2 {:width 1 :height 1}
          dim3 {:width 1 :height 2}
          t #(is (= %2 (greater-dimension? %1)))]
      (t dim1 [:width])
      (t dim2 [:width :height])
      (t dim3 [:height]))))

(deftest test-generate-big-png
  (testing "Testing that a big png file is correctly generated."
    (let [t (generate-big-png testsvg (str testdir "big-png1.png"))]
      (is (= clojure.lang.PersistentArrayMap (type t)))
      (is (png? (:file t)))
      (is (= "ffffff00" (:background t)))
      (is (= 12100 (:width t)))
      (is (= 8519 (:height t))))))

(deftest test-generate-big-padded-png
  (testing "Testing that the final size of the image is correct."
    (let [t (generate-big-padded-png testsvg (str testdir "padded.png"))]
      (is (= java.lang.String (type t)))
      (is (png? t))
      (is (= 13500 (:width (png-dimensions? t)))))))
