let rec shift lst n =
  let addToEnd lst = match lst with
    | [] -> []
    | h::tl -> tl @ [h]

  in if n > 0 then
    shift (addToEnd lst) (n-1)
  else
    lst ;;

let print_lst lst =
  let rec print_lst_helper lst =
    match lst with
    | [] -> ()
    | h::tl -> print_int h; print_string " "; print_lst_helper tl in
  print_lst_helper lst; print_string "\n";;

let lst = [1; 2; 3; 4];;

print_lst (shift lst 0);;
print_lst (shift lst 1);;
print_lst (shift lst 2);;
print_lst (shift lst 3);;
print_lst (shift lst 4);;