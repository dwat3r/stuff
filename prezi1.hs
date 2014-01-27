import Data.Char
import System.IO

main = do
	withFile "test.txt" ReadMode (\handle -> do
	input <- hGetContents handle 
	let nums = numbers $ last $ lines input
	let k = last $ numbers $ head $ lines input
	putStrLn $ show  $ length [(fst i,j)|i<-(zip nums [0..]),j<-(drop (snd i) nums),abs(fst i-j)==k])

numbers::String -> [Integer]
numbers = map read . words
